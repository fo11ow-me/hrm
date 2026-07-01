package com.qiujie.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiujie.common.llm.LlmProvider;
import com.qiujie.assistant.entity.AssistantMessage;
import com.qiujie.assistant.entity.AssistantSession;
import com.qiujie.assistant.entity.AssistantSessionContext;
import com.qiujie.assistant.mapper.AssistantMessageMapper;
import com.qiujie.assistant.mapper.AssistantSessionContextMapper;
import com.qiujie.assistant.mapper.AssistantSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 助手短期记忆三级压缩服务。
 * <p>
 * 背景问题：LLM 上下文窗口有限（qwen-plus 约 32K tokens），
 * 对话超过一定轮数后无法把全部历史塞进 prompt。同时每次请求都带全量历史会产生大量冗余 token 消耗。
 * 解决方案：分层压缩——把历史对话逐步提炼为要点摘要，只注入浓缩后的记忆到 LLM 上下文。
 * </p>
 *
 * <h3>三级压缩模型</h3>
 * <table>
 *   <tr><th>层级</th><th>名称</th><th>触发间隔</th><th>存储位置</th><th>作用</th></tr>
 *   <tr><td>L1</td><td>会话记忆 (sessionMemory)</td><td>每 5 条消息</td><td>assistant_session_context.session_memory</td><td>LLM 将最近对话总结为关键信息要点</td></tr>
 *   <tr><td>L2</td><td>紧凑摘要 (compactSummary)</td><td>每 15 条消息</td><td>assistant_session_context.compact_summary</td><td>LLM 将 L1 记忆进一步压缩为 300 字摘要</td></tr>
 *   <tr><td>L3</td><td>运行时截断</td><td>每次调用</td><td>无持久化</td><td>如果总 token 超阈值只保留最近 N 条消息</td></tr>
 * </table>
 *
 * <h3>并发安全</h3>
 * L1/L2 更新使用乐观锁（context_version 字段）：
 * UPDATE WHERE context_version = expected，失败则重读最新版本后重试，最多 3 次。
 * 避免悲观锁（SELECT FOR UPDATE）在长事务中持有的风险。
 *
 * @author quuj
 */
@Service // Spring 单例 Bean，由 AgentService 注入调用
public class AssistantMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AssistantMemoryService.class);

    @Autowired
    private AssistantSessionMapper sessionMapper; // 读取会话的 messageCount 判断阈值

    @Autowired
    private AssistantMessageMapper messageMapper; // 读取历史消息作为压缩输入

    @Autowired
    private AssistantSessionContextMapper contextMapper; // 读写 session_memory / compact_summary

    @Autowired(required = false) // required=false：允许项目未配置 LLM 时启动，记忆功能静默降级
    private LlmProvider llmClient; // 用于生成摘要的 LLM 接口，与对话主链路复用同一配置

    @Value("${agent.memory.l1-update-interval:5}") // 从配置文件注入，默认 5 条消息触发一次 L1 记忆更新
    private int l1UpdateInterval;

    @Value("${agent.memory.l2-update-interval:15}") // 默认 15 条消息触发一次 L2 紧凑摘要
    private int l2UpdateInterval;

    @Value("${agent.memory.max-tokens:50000}") // 默认 50000 token 触发 L3 截断
    private int maxTokens;

    @Value("${agent.memory.keep-recent:5}") // 截断时保留最近 5 条消息
    private int keepRecent;

    /**
     * 构建注入 LLM prompt 的系统上下文。
     * <p>
     * 拼接顺序：compactSummary（长期压缩）→ sessionMemory（近期要点），
     * 将压缩记忆作为系统背景信息注入，减少原始消息的 token 消耗。
     * 当前 AgentService.chatSync() 未调用此方法——是预留的显式上下文注入入口，
     * 实际上下文通过 ChatClient 的默认消息历史机制管理。
     * </p>
     *
     * @param session 当前会话
     * @return 拼接后的上下文字符串，无上下文时返回空串
     */
    public String buildContext(AssistantSession session) {
        AssistantSessionContext ctx = contextMapper.selectById(session.getId()); // 主键查询上下文记录
        if (ctx == null) return ""; // 防御：理论上新建会话时同步创建了 context，不会为 null

        StringBuilder sb = new StringBuilder();

        // 先拼 L2 紧凑摘要——长期背景，提供全局视角
        if (ctx.getCompactSummary() != null && !ctx.getCompactSummary().isBlank()) {
            sb.append("【历史对话精要】\n").append(ctx.getCompactSummary()).append("\n\n");
        }

        // 再拼 L1 会话记忆——近期细节，提供当前上下文
        if (ctx.getSessionMemory() != null && !ctx.getSessionMemory().isBlank()) {
            sb.append("【当前会话关键信息】\n").append(ctx.getSessionMemory()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 获取最近消息（含 L3 运行时截断）。
     * <p>
     * 先加载会话全部消息，按字符数估算 token 数（中英文混合场景下 ~2 字符/token），
     * 超过阈值时只保留最近 keepRecent 条——这是硬截断，不经过 LLM 压缩。
     * L3 是 L1/L2 的后备防线：消息增速超过压缩频率时兜底防 OOM / API token limit。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 截断后的消息列表（可能是全量或最近 N 条）
     */
    public List<AssistantMessage> getRecentMessages(Long sessionId) {
        List<AssistantMessage> all = messageMapper.selectList(
                new QueryWrapper<AssistantMessage>().eq("session_id", sessionId)
                        .orderByAsc("id")); // 按时间顺序拿到完整对话链

        // 估算 token 数：简单按 content.length / 2，对于中英文混合场景是合理的近似
        long estimatedTokens = all.stream().mapToLong(m ->
                m.getContent() != null ? m.getContent().length() / 2 : 0).sum();
        if (estimatedTokens > maxTokens) {
            log.warn("Session {} token overflow: {} > {}, truncating", sessionId, estimatedTokens, maxTokens);
            // subList 返回视图，原 list 不会被 GC 但这里方法结束即释放引用，无内存风险
            return all.subList(Math.max(0, all.size() - keepRecent), all.size());
        }
        return all; // 未超阈值，全量返回
    }

    /**
     * 消息发送后的记忆更新入口。
     * <p>
     * 由 AgentService 在每次 LLM 调用完成后调用。内部按 messageCount 对阈值取模判断
     * 是否触发压缩——不是每条消息都调 LLM 做摘要，而是每 N 条触发一次，节省 token 开销。
     * </p>
     *
     * @param session 当前会话，已更新 messageCount
     */
    public void afterMessage(AssistantSession session) {
        // 重新计数：messageCount 是 AgentService 传入时已 +2 后的值，这里 +1 是冗余保护
        int count = session.getMessageCount() != null ? session.getMessageCount() + 1 : 1;
        session.setMessageCount(count);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 阈值检查：count % interval == 0 触发压缩
        if (count % l1UpdateInterval == 0) {
            updateSessionMemory(session); // L1：LLM 生成会话要点记忆
        }

        if (count % l2UpdateInterval == 0) {
            updateCompactSummary(session); // L2：LLM 压缩 L1 记忆为精炼摘要
        }
    }

    /**
     * L1 会话记忆更新。
     * <p>
     * 流程：取出最近消息文本 → 拼接已有记忆 + 新对话 → 让 LLM 生成更新后的记忆要点。
     * 已有记忆不被丢弃而是作为输入的一部分，实现增量更新而非全量重算。
     * llmClient 为 null 时静默跳过（未配置 LLM 的环境）。
     * </p>
     */
    private void updateSessionMemory(AssistantSession session) {
        if (llmClient == null) return; // LLM 未配置时静默降级，不影响主对话链路
        List<AssistantMessage> recent = getRecentMessages(session.getId()); // L3 截断后再取消息
        if (recent.size() < l1UpdateInterval) return; // 消息不够 N 条不触发压缩

        // 拼接对话文本：role + content 逐行，给 LLM 足够的上下文
        StringBuilder history = new StringBuilder();
        for (AssistantMessage m : recent) {
            history.append(m.getRole()).append(": ").append(m.getContent()).append("\n");
        }

        AssistantSessionContext ctx = contextMapper.selectById(session.getId());
        if (ctx == null) return;

        String existing = ctx.getSessionMemory() != null ? ctx.getSessionMemory() : ""; // 已有记忆作为输入保留
        // 提示词设计：明确要求输出要点形式、中文、不超过 500 字，控制输出长度
        String prompt = String.format(
                "以下是对话历史。请以要点形式总结新增的关键事实、决策和用户偏好。保留已有记忆中的重要信息。\n\n"
                + "已有记忆：\n%s\n\n新增对话：\n%s\n\n更新后的完整记忆（要点形式，中文，不超过500字）：",
                existing, history.toString());
        try {
            String updated = llmClient.generate(prompt, ""); // 调用 LLM 生成摘要
            if (updated != null && !updated.isBlank()) {
                // 乐观锁更新：CAS contextVersion，冲突自动重试
                updateContext(session.getId(), ctx.getContextVersion(), w -> {
                    w.set(AssistantSessionContext::getSessionMemory, updated.trim());
                    w.set(AssistantSessionContext::getContextVersion, ctx.getContextVersion() + 1);
                });
            }
        } catch (Exception e) {
            log.warn("L1 memory update failed: {}", e.getMessage()); // 记忆更新失败不阻断主流程
        }
    }

    /**
     * L2 紧凑摘要更新。
     * <p>
     * 输入是 L1 的 sessionMemory（已经是摘要），进一步压缩到 300 字。
     * 这样两级压缩后的信息密度远高于原始对话，在有限 token 预算内保留更多历史信息。
     * </p>
     */
    private void updateCompactSummary(AssistantSession session) {
        if (llmClient == null) return;
        AssistantSessionContext ctx = contextMapper.selectById(session.getId());
        // L2 依赖 L1：没有 sessionMemory 就没东西可压缩
        if (ctx == null || ctx.getSessionMemory() == null || ctx.getSessionMemory().isBlank()) return;

        String existing = ctx.getCompactSummary() != null ? ctx.getCompactSummary() : "";
        String prompt = String.format(
                "请将以下会话记忆压缩为一段精炼的摘要（最多300字），保留最重要的信息。\n\n"
                + "已有摘要：%s\n\n会话记忆：%s\n\n精炼摘要：",
                existing, ctx.getSessionMemory()); // 已有摘要作为输入，避免每次重算丢失早期信息
        try {
            String compressed = llmClient.generate(prompt, "");
            if (compressed != null && !compressed.isBlank()) {
                updateContext(session.getId(), ctx.getContextVersion(), w -> {
                    w.set(AssistantSessionContext::getCompactSummary, compressed.trim());
                    w.set(AssistantSessionContext::getContextVersion, ctx.getContextVersion() + 1);
                });
            }
        } catch (Exception e) {
            log.warn("L2 compact summary failed: {}", e.getMessage());
        }
    }

    /**
     * 乐观锁上下文更新。
     * <p>
     * 不锁行，而是 UPDATE WHERE context_version = expectedVersion。
     * 如果 rows=0 说明被其他线程抢先更新了，重新读取最新版本号后重试。
     * 最多重试 3 次，超过则认为冲突过于频繁，放弃本次更新。
     * 相比悲观锁（SELECT FOR UPDATE），乐观锁在低冲突场景下性能更好且无死锁风险。
     * </p>
     *
     * @param sessionId       会话 ID
     * @param expectedVersion 期望的当前版本号
     * @param updater         设置更新字段的 Lambda，由调用方决定更新哪些字段
     */
    private void updateContext(Long sessionId, long expectedVersion,
                               java.util.function.Consumer<LambdaUpdateWrapper<AssistantSessionContext>> updater) {
        int maxRetries = 3; // 最多重试 3 次，防止无限循环
        for (int i = 0; i < maxRetries; i++) {
            LambdaUpdateWrapper<AssistantSessionContext> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(AssistantSessionContext::getSessionId, sessionId); // WHERE session_id = ?
            wrapper.eq(AssistantSessionContext::getContextVersion, expectedVersion); // AND context_version = ? —— 乐观锁条件
            updater.accept(wrapper); // 调用方设置 SET 子句（如 session_memory = ?, context_version = ?）
            int rows = contextMapper.update(wrapper); // 实际执行 SQL，返回受影响行数
            if (rows > 0) return; // rows=1 表示更新成功，rows=0 表示版本号已变（被其他线程抢先）

            // —— 冲突重试 ——
            AssistantSessionContext latest = contextMapper.selectById(sessionId); // 重新读取最新版本
            if (latest == null) return; // 上下文记录已被删除，放弃更新
            expectedVersion = latest.getContextVersion(); // 更新为最新版本号，下一轮重试用
            log.debug("Context update conflict for session {}, retry {}/{}", sessionId, i + 1, maxRetries);
        }
        log.warn("Context update failed after {} retries for session {}", maxRetries, sessionId); // 3 次重试全部失败
    }
}
