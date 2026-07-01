package com.qiujie.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.entity.ChatSessionContext;
import com.qiujie.assistant.mapper.ChatMessageMapper;
import com.qiujie.assistant.mapper.ChatSessionMapper;
import com.qiujie.assistant.mapper.ChatSessionContextMapper;
import com.qiujie.assistant.memory.ChatMemoryProperties;
import com.qiujie.assistant.memory.ChatMemorySummarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 助手短期记忆三级压缩服务。
 * <p>
 * L1 会话记忆（每 4 条或 1200 token）→ L2 紧凑摘要（总 token &gt; 6500 且增量达标）→
 * L3 运行时截断（BEFORE_MODEL Hook 或内联检查）。
 * 使用范围追踪增量更新 + 乐观锁并发写入。
 * </p>
 *
 * @author quuj
 */
@Service // 声明为 Spring Bean，默认单例，由 ChatService 注入调用
public class ChatMemoryService {

    /** 日志门面，实际输出到 Logback */
    private static final Logger log = LoggerFactory.getLogger(ChatMemoryService.class);

    /** token 估算除数：中英文混合场景下约 2 字符 ≈ 1 token */
    static final int TOKEN_DIVISOR = 2;

    // 以下四个依赖均为构造注入的不可变字段，保证线程安全

    /** 操作 ast_chat_session 表，用于更新 messageCount / updateTime */
    private final ChatSessionMapper sessionMapper;

    /** 操作 ast_chat_message 表，用于查询会话的全部历史消息 */
    private final ChatMessageMapper messageMapper;

    /** 操作 ast_chat_session_context 表，用于读写 L1/L2 记忆和摘要 */
    private final ChatSessionContextMapper contextMapper;

    /** LLM 驱动的摘要生成器，将消息列表压缩为要点文本 */
    private final ChatMemorySummarizer summarizer;

    /** 所有可调参数的外部化配置（阈值、间隔、保留数） */
    private final ChatMemoryProperties props;

    /**
     * 构造注入所有依赖。
     * 构造注入优于字段注入：依赖明确不可变，方便单元测试 mock。
     */
    public ChatMemoryService(ChatSessionMapper sessionMapper,
            ChatMessageMapper messageMapper,
            ChatSessionContextMapper contextMapper,
            ChatMemorySummarizer summarizer,
            ChatMemoryProperties props) {
        this.sessionMapper = sessionMapper;     // 会话表 Mapper
        this.messageMapper = messageMapper;     // 消息表 Mapper
        this.contextMapper = contextMapper;     // 上下文表 Mapper
        this.summarizer = summarizer;           // LLM 摘要器
        this.props = props;                     // 配置属性
    }

    /**
     * 组装注入 LLM 的系统上下文。
     * <p>
     * 拼接顺序：L2 长期记忆 → L1 近期记忆。
     * L2 提供跨长对话的全局背景，L1 提供当前会话的近期细节。
     * 当 context 不存在或无有效记忆时返回空字符串。
     * </p>
     *
     * @param session 当前会话
     * @return 拼接后的系统上下文文本，无内容时返回空串
     */
    public String buildContext(ChatSession session) {
        // 按主键 session_id 查询上下文记录（一对一关系）
        ChatSessionContext ctx = contextMapper.selectById(session.getId());
        if (ctx == null) {
            return "";                // 防御：理论上新建会话时同步创建了 context，不会为 null
        }
        StringBuilder sb = new StringBuilder(); // 用 StringBuilder 避免多次字符串拼接产生中间对象

        // 先拼 L2 紧凑摘要——跨长对话的全局背景
        if (ctx.getCompactSummary() != null && !ctx.getCompactSummary().isBlank()) {
            sb.append("【历史对话精要】\n")
              .append(ctx.getCompactSummary())
              .append("\n\n");        // 双换行分隔，与 L1 记忆有视觉间隔
        }

        // 再拼 L1 会话记忆——当前会话的近期细节
        if (ctx.getSessionMemory() != null && !ctx.getSessionMemory().isBlank()) {
            sb.append("【当前会话关键信息】\n")
              .append(ctx.getSessionMemory())
              .append("\n\n");
        }

        return sb.toString();         // 可能为空串，调用方需判断
    }

    /**
     * 消息发送后的记忆更新入口。
     * <p>
     * 由 ChatService 在 LLM 调用完成后调用。内部按 token 阈值判断是否触发压缩，
     * 不是每条消息都调 LLM——节省 token 开销。
     * </p>
     *
     * @param session          当前会话（已更新 messageCount）
     * @param toolMode         当前工具模式 CHAT / KB_SEARCH
     * @param currentMessageId 当前轮次最后一条消息的主键 ID
     */
    public void afterMessage(ChatSession session, String toolMode, Long currentMessageId) {
        maintain(session, toolMode, currentMessageId); // 委托给私有核心方法
    }

    /**
     * 记忆维护核心逻辑。
     * <p>
     * 完整流程：
     * <ol>
     *   <li>加载会话全部消息，按 id 升序排列</li>
     *   <li>通过 range_end_message_id 识别自上次压缩以来的新消息</li>
     *   <li>判断是否满足 L1 触发条件（消息数或 token 数达标）</li>
     *   <li>调用 LLM 生成更新后的会话记忆</li>
     *   <li>判断是否满足 L2 触发条件，满足则生成紧凑摘要</li>
     *   <li>乐观锁写入 context（CAS context_version）</li>
     *   <li>更新 session 的 messageCount 和 updateTime</li>
     * </ol>
     * </p>
     */
    private void maintain(ChatSession session, String toolMode, Long currentMessageId) {
        // —— 加载全部消息，按主键升序（id 自增等价于时间序）——
        List<ChatMessage> allMessages = messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", session.getId())   // WHERE session_id = ?
                        .orderByAsc("id"));                   // ORDER BY id ASC

        if (allMessages.isEmpty()) {
            return;                    // 无消息则无需维护
        }

        // —— 查询当前上下文，获取上次压缩的终点消息 ID ——
        ChatSessionContext ctx = contextMapper.selectById(session.getId());

        // lastRangeEnd：上次 L1 压缩覆盖的最后一 条消息 ID。
        // 为 0 表示从未压缩过，所有消息都是"新消息"。
        long lastRangeEnd = ctx != null && ctx.getSessionMemoryRangeEndMessageId() != null
                ? ctx.getSessionMemoryRangeEndMessageId()
                : 0L;                  // 默认 0：所有消息 id > 0，全部视为新消息

        // —— 增量过滤：只取 id > lastRangeEnd 的新消息 ——
        // 这是"范围追踪"的核心——已压缩过的旧消息不再参与摘要输入
        List<ChatMessage> newMessages = allMessages.stream()
                .filter(m -> m.getId() != null && m.getId() > lastRangeEnd)
                .collect(Collectors.toList()); // 收集到新列表

        // —— L1 触发判断：消息数或 token 数达标才触发 ——
        if (!shouldUpdateSessionMemory(newMessages)) {
            return;                    // 未达阈值，跳过本次压缩
        }

        // —— 上下文行不存在时先插入 ——
        // 理论上 ChatService 创建会话时已同步插入，这里是防御性处理
        if (ctx == null) {
            ctx = newContext(session.getId());    // 构造初始上下文对象
            contextMapper.insert(ctx);             // INSERT 新行
            ctx = contextMapper.selectById(session.getId()); // 重读以获取数据库默认值
            if (ctx == null) {
                return;                // 插入失败，静默退出
            }
        }

        // —— L1 更新：调用 LLM 生成新的会话记忆 ——
        // 已有记忆 + 新消息 → LLM → 更新后的要点记忆
        ChatSessionContext toWrite = ctx; // toWrite 是 ctx 的引用，后续 set 直接修改字段
        toWrite.setSessionMemory(summarizer.summarizeSessionMemory(
                ctx.getSessionMemory(),    // 已有记忆作为输入，实现增量更新
                newMessages,               // 自上次压缩以来的新消息
                toolMode));                // 当前工具模式，影响 LLM 摘要视角
        toWrite.setSessionMemoryBaseMessageId(newMessages.get(0).getId()); // 本次 L1 覆盖的首条消息 ID
        toWrite.setSessionMemoryRangeEndMessageId(
                newMessages.get(newMessages.size() - 1).getId()); // 本次 L1 覆盖的末条消息 ID
        toWrite.setUpdateTime(LocalDateTime.now());               // 记录最后更新时间

        // —— token 估算，用于 L2 触发判断 ——
        int newTokens = estimateTokens(newMessages);     // 新增 token

        // 乐观锁版本号：读时版本 + 1
        long expectedVersion = ctx.getContextVersion() != null
                ? ctx.getContextVersion()
                : 0L;
        toWrite.setContextVersion(expectedVersion + 1); // 写入时版本号 +1

        // —— L2：FIFO 追加模式，从新消息独立提取要点 ——
        if (shouldCompactSession(newMessages.size(), newTokens)) {
            String newChunk = summarizer.summarizeCompactSummary(
                    ctx.getCompactSummary(), newMessages, toolMode);
            if (!"NONE".equals(newChunk)) {
                String current = ctx.getCompactSummary() != null ? ctx.getCompactSummary() : "";
                String merged = current + "\n" + newChunk;

                // 超 token 阈值时从头部丢弃，找标点边界保证句子完整
                int estimatedTokens = merged.length() / TOKEN_DIVISOR;
                if (estimatedTokens > props.getCompactSummaryMaxTokens()) {
                    int cutoffTokens = estimatedTokens - props.getCompactSummaryMaxTokens();
                    int cutFrom = cutoffTokens * TOKEN_DIVISOR;
                    for (int i = cutFrom; i < merged.length(); i++) {
                        char c = merged.charAt(i);
                        if (c == '。' || c == '\n' || c == '！' || c == '？' || c == '.') {
                            cutFrom = i + 1;
                            break;
                        }
                    }
                    merged = merged.substring(cutFrom);
                }

                toWrite.setCompactSummary(merged);
                toWrite.setCompactSummaryBaseMessageId(allMessages.get(0).getId());
                toWrite.setCompactSummaryRangeEndMessageId(
                        newMessages.get(newMessages.size() - 1).getId());
            }
        }

        // —— 乐观锁 CAS 写入 ——
        // 将 snapshot 的 expectedVersion 捕获为 final 变量供 Lambda 使用
        long writeVersion = expectedVersion;
        updateContext(session.getId(), writeVersion, w -> {
            // LambdaUpdateWrapper 自动将 getter 方法引用解析为 @TableField 对应的数据库列名
            w.set(ChatSessionContext::getSessionMemory, toWrite.getSessionMemory());
            w.set(ChatSessionContext::getSessionMemoryBaseMessageId,
                    toWrite.getSessionMemoryBaseMessageId());
            w.set(ChatSessionContext::getSessionMemoryRangeEndMessageId,
                    toWrite.getSessionMemoryRangeEndMessageId());
            w.set(ChatSessionContext::getCompactSummary, toWrite.getCompactSummary());
            w.set(ChatSessionContext::getCompactSummaryBaseMessageId,
                    toWrite.getCompactSummaryBaseMessageId());
            w.set(ChatSessionContext::getCompactSummaryRangeEndMessageId,
                    toWrite.getCompactSummaryRangeEndMessageId());
            w.set(ChatSessionContext::getContextVersion, toWrite.getContextVersion());
            w.set(ChatSessionContext::getUpdateTime, toWrite.getUpdateTime());
        });

        // —— 更新会话统计 ——
        // messageCount +2：USER 消息 + ASSISTANT 消息各计一条
        session.setMessageCount(session.getMessageCount() != null
                ? session.getMessageCount() + 2
                : 2);                  // 防御 null：首次计数时从 2 开始
        session.setUpdateTime(LocalDateTime.now()); // 更新会话最后活跃时间
        sessionMapper.updateById(session);          // 按主键更新 session 表
    }

    /**
     * 判断是否需要触发 L1 会话记忆更新。
     * <p>
     * 满足任一条件即触发：(1) 新消息数 ≥ 配置阈值 (默认 4)；
     * (2) 新增 token 数 ≥ 配置阈值 (默认 1200)。
     * 双重条件确保短消息攒够了再压缩，长消息及时压缩。
     * </p>
     *
     * @param newMessages 自上次压缩以来的新消息列表
     * @return true 需更新，false 跳过
     */
    private boolean shouldUpdateSessionMemory(List<ChatMessage> newMessages) {
        if (newMessages.isEmpty()) {
            return false;              // 无新消息，无需更新
        }
        int tokens = estimateTokens(newMessages); // 估算新增消息的 token 数
        return newMessages.size() >= props.getL1MessageTrigger()  // 消息数达标
                || tokens >= props.getL1TokenTrigger();           // 或 token 数达标
    }

    /**
     * 判断是否需要触发 L2 紧凑摘要。
     * <p>
     * 必须同时满足两个条件：(1) 会话总 token 超阈值 (默认 6500)；
     * (2) 增量达标——新消息数或新增 token 超阈值。
     * 第一层门槛避免对话太短时过早压缩；第二层门槛控制触发频率。
     * </p>
     *
     * @param totalTokens  会话全部消息的估算 token 总数
     * @param newMsgCount  新增消息数
     * @param newTokens    新增 token 估算值
     * @return true 需生成紧凑摘要，false 跳过
     */
    private boolean shouldCompactSession(int newMsgCount, int newTokens) {
        return newMsgCount >= props.getL2MessageTrigger()              // 新消息数达标
                || newTokens >= props.getL2TokenTrigger();              // 或新增 token 达标
    }

    /**
     * 估算消息列表的 token 数。
     * <p>
     * 简单按字符数 / 4 估算，中英文混合场景下是合理的近似值。
     * 仅为触发判断提供参考，不需要精确计数。
     * </p>
     *
     * @param messages 消息列表
     * @return 估算 token 数，空列表返回 0
     */
    public static int estimateTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;                  // 空列表无 token
        }
        int totalChars = messages.stream()
                .map(ChatMessage::getContent)          // 提取每条消息的文本内容
                .filter(c -> c != null && !c.isBlank()) // 跳过空内容（role=TOOL 可能无文本）
                .mapToInt(String::length)               // 获取字符串长度
                .sum();                                  // 累加所有字符数
        return Math.max(1, totalChars / TOKEN_DIVISOR); // 至少返回 1，避免除零边界
    }

    /**
     * 创建初始上下文对象。
     * <p>
     * 仅设置 sessionId 和初始版本号 0，其余字段留 NULL。
     * 后续 L1/L2 更新时逐步填充。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 初始化的 ChatSessionContext
     */
    private ChatSessionContext newContext(Long sessionId) {
        ChatSessionContext ctx = new ChatSessionContext();
        ctx.setSessionId(sessionId);           // 与 session 表主键一一对应
        ctx.setContextVersion(0L);             // 乐观锁版本号从 0 开始
        ctx.setUpdateTime(LocalDateTime.now());// 记录创建时间
        return ctx;
    }

    /**
     * 乐观锁上下文更新。
     * <p>
     * 不使用 SELECT FOR UPDATE（悲观锁），而是 CAS 写入：
     * {@code UPDATE WHERE context_version = expectedVersion}。
     * 如果 affectedRows = 0，说明被其他线程抢先更新了——
     * 重新读取最新版本号后重试，最多 3 次。
     * 适用于低冲突场景，无死锁风险且性能更好。
     * </p>
     *
     * @param sessionId       会话 ID
     * @param expectedVersion 期望的当前版本号（读时版本）
     * @param updater         消费者 Lambda，由调用方决定更新哪些字段
     */
    private void updateContext(Long sessionId, long expectedVersion,
            Consumer<LambdaUpdateWrapper<ChatSessionContext>> updater) {
        for (int i = 0; i < 3; i++) {          // 最多重试 3 次，防止无限循环
            // 构建 UPDATE ... WHERE 条件
            LambdaUpdateWrapper<ChatSessionContext> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ChatSessionContext::getSessionId, sessionId);       // WHERE session_id = ?
            wrapper.eq(ChatSessionContext::getContextVersion, expectedVersion); // AND context_version = ? —— CAS 条件

            // 调用方设置 SET 子句（如 session_memory = ?, context_version = ?）
            updater.accept(wrapper);

            // 执行 SQL，返回受影响行数
            int rows = contextMapper.update(null, wrapper);

            if (rows > 0) {
                return;                // rows=1：CAS 成功，版本号已更新
            }
            // rows=0：版本号已被其他线程修改，CAS 冲突

            // 重新读取最新版本号，准备重试
            ChatSessionContext latest = contextMapper.selectById(sessionId);
            if (latest == null) {
                return;                // 上下文记录已被删除，放弃更新
            }
            expectedVersion = latest.getContextVersion(); // 更新为最新版本号
            // 继续循环，下一轮用新版本号重试
        }
        // 3 次重试全部失败——冲突过于频繁，记录警告日志
        log.warn("Context update failed after 3 retries for session {}", sessionId);
    }
}
