package com.qiujie.assistant.service;

import com.qiujie.assistant.ChatTools;                       // @Tool 方法集合，注册为 function calling 工具
import com.qiujie.assistant.dto.ChatRequest;                 // 前端 JSON → DTO（message, sessionId, mode）
import com.qiujie.assistant.entity.ChatMessage;              // 映射 ast_chat_message 表
import com.qiujie.assistant.entity.ChatSession;              // 映射 ast_chat_session 表
import com.qiujie.assistant.entity.ChatSessionContext;       // 映射 ast_chat_session_context 表（L1/L2 记忆）
import com.qiujie.assistant.mapper.ChatMessageMapper;        // MyBatis-Plus BaseMapper<ChatMessage>
import com.qiujie.assistant.mapper.ChatSessionMapper;        // MyBatis-Plus BaseMapper<ChatSession>
import com.qiujie.assistant.mapper.ChatSessionContextMapper; // MyBatis-Plus BaseMapper<ChatSessionContext>
import com.qiujie.assistant.memory.ChatMemoryProperties;      // chat.memory.* 结构化配置（阈值/间隔/保留数）
import com.qiujie.assistant.memory.ChatSessionSummaryService; // 前端展示用非 LLM 会话摘要
import com.qiujie.util.SecurityUtil;                          // 从 JWT SecurityContext 提取当前员工 staffId
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;         // Spring AI 统一 LLM 调用入口
import org.springframework.ai.chat.messages.AssistantMessage; // Spring AI 助手消息类型（注：不是我们的 ChatMessage）
import org.springframework.ai.chat.messages.Message;          // Spring AI 消息接口
import org.springframework.ai.chat.messages.UserMessage;      // Spring AI 用户消息类型
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 声明式事务
import org.springframework.transaction.support.TransactionTemplate; // 编程式事务——SSE 异步线程使用
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter; // SSE 长连接推送

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 助手核心编排引擎。
 * <p>
 * 职责：管理对话生命周期（会话创建→消息持久化→LLM 调用→记忆更新→SSE 推送）。
 * LLM 交互基于 Spring AI 的 {@link ChatClient}，工具集通过 {@link ChatTools} 注入。
 * </p>
 *
 * @author quuj
 */
@Service // Spring 单例 Bean，由 ChatController 注入调用
public class ChatService {

    /** 日志门面，输出到 Logback */
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    // ===== 构造注入的不可变依赖，保证线程安全 =====

    /** 操作 ast_chat_session 表 */
    private final ChatSessionMapper sessionMapper;
    /** 操作 ast_chat_message 表 */
    private final ChatMessageMapper messageMapper;
    /** 操作 ast_chat_session_context 表（L1/L2 记忆存储） */
    private final ChatSessionContextMapper contextMapper;
    /** 三级记忆压缩服务（L1 会话记忆 / L2 紧凑摘要 / L3 截断） */
    private final ChatMemoryService memoryService;
    /** 前端展示用非 LLM 会话摘要服务 */
    private final ChatSessionSummaryService summaryService;
    /** JWT 认证工具，提取当前登录员工 ID */
    private final SecurityUtil securityUtil;
    /** 记忆模块可调参数（阈值、间隔、保留数） */
    private final ChatMemoryProperties memoryProps;
    /** Spring AI ChatClient，封装 DashScope qwen-plus 调用 */
    private final ChatClient chatClient;
    /** @Tool 注解的方法集合，作为 function calling 工具注入 LLM */
    private final ChatTools chatTools;
    /** 编程式事务——SSE 异步线程中管理持久化事务 */
    private final TransactionTemplate txTemplate;

    /**
     * 构造注入所有依赖。
     * 构造注入优于字段注入：依赖明确不可变，方便单元测试 mock。
     */
    public ChatService(ChatSessionMapper sessionMapper,
            ChatMessageMapper messageMapper,
            ChatSessionContextMapper contextMapper,
            ChatMemoryService memoryService,
            ChatSessionSummaryService summaryService,
            SecurityUtil securityUtil,
            ChatMemoryProperties memoryProps,
            ChatClient.Builder chatClientBuilder, // Spring AI 自动配置提供的 Builder
            ChatTools chatTools,
            TransactionTemplate txTemplate) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.contextMapper = contextMapper;
        this.memoryService = memoryService;
        this.summaryService = summaryService;
        this.securityUtil = securityUtil;
        this.memoryProps = memoryProps;
        this.chatClient = chatClientBuilder.build(); // build() 后 ChatClient 线程安全可复用
        this.chatTools = chatTools;
        this.txTemplate = txTemplate; // SSE 异步线程用编程式事务
    }

    /**
     * SSE 流式对话——核心方法。
     * <p>
     * 完整流程：创建/复用会话 → 持久化用户消息 → 组装 L1/L2 记忆 + L3 截断 → 调用 LLM →
     * 异步逐字 SSE 推送 → 持久化助手消息 → 触发记忆更新。
     * </p>
     *
     * @param request 前端 JSON（含 message、可选 sessionId、可选 mode）
     * @return SseEmitter，Spring MVC 将其管理为 SSE 长连接（5 分钟超时）
     */
    @Transactional // 声明式事务：方法内 DB 操作在同一事务中，RuntimeException 时回滚
    public SseEmitter chat(ChatRequest request) {
        // SSE 发射器，5 分钟超时防止僵尸连接
        SseEmitter emitter = new SseEmitter(300000L);

        // 从 JWT Token 提取当前员工 ID，保证跨用户数据隔离
        Integer staffId = securityUtil.getCurrentOperatorId();

        // 保存当前认证上下文——后续异步 SSE 推送线程不在请求线程内，
        // SecurityContext 默认线程隔离，需手动传递
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        // 复用已有会话或新建（新建时同步创建 context 行）
        ChatSession session = getOrCreateSession(request, staffId);

        // —— 持久化用户消息 ——
        // 先于 LLM 调用写入，确保即使 LLM 超时用户消息也不丢失
        ChatMessage userMsg = new ChatMessage()
                .setSessionId(session.getId())
                .setRole("USER")              // 角色：USER / ASSISTANT / TOOL
                .setContent(request.getMessage()) // 原文存储，用于记忆压缩和回显
                .setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);         // MyBatis-Plus 自动生成 INSERT SQL

        // —— 构建记忆上下文 ——
        // 从 L1/L2 压缩记忆中提取当前会话的关键信息，注入到 system prompt
        String memoryContext = memoryService.buildContext(session);

        // —— L3 运行时截断 ——
        // 加载会话全部消息，估算 token 数，超阈值时硬截断只保留最近 N 条
        List<ChatMessage> recentMessages = loadRecentMessages(session.getId());
        int estimatedTokens = memoryService.estimateTokens(recentMessages);
        if (estimatedTokens > memoryProps.getMaxTokens()) {   // 默认 50000 token
            int keep = memoryProps.getKeepRecent();             // 默认保留 3 条
            recentMessages = recentMessages.subList(
                    Math.max(0, recentMessages.size() - keep), // 防越界
                    recentMessages.size());
            log.warn("Session {} L3 truncation: {} > {} tokens, kept {} messages",
                    session.getId(), estimatedTokens, memoryProps.getMaxTokens(), keep);
        }

        // —— 调用 LLM ——
        String answer;
        try {
            // 将 DB 消息实体转换为 Spring AI 的 Message 类型
            List<Message> historyMessages = new ArrayList<>();
            for (ChatMessage m : recentMessages) {
                if (m.getContent() == null || m.getContent().isBlank()) continue; // 跳过空消息
                if ("USER".equals(m.getRole())) {
                    historyMessages.add(new UserMessage(m.getContent()));       // Spring AI 用户消息
                } else if ("ASSISTANT".equals(m.getRole())) {
                    historyMessages.add(new AssistantMessage(m.getContent()));  // Spring AI 助手消息
                }
            }

            // L3 与 L2 去重：L2 摘要已覆盖全局，截断后跳过重叠的原始消息
            if (memoryContext != null && !memoryContext.isBlank()) {
                historyMessages.clear();
            }

            // 构建 Prompt：历史消息 + 当前问题 + function calling 工具集
            var prompt = chatClient.prompt()
                    .messages(historyMessages)
                    .user(request.getMessage())
                    .tools(chatTools);
            if (memoryContext != null && !memoryContext.isBlank()) {
                prompt = prompt.system(s -> s.text(memoryContext)); // L1+L2 记忆作为 system prompt
            }
            answer = prompt.call().content();           // 同步阻塞调用，返回完整回答文本
        } catch (Exception e) {
            log.error("LLM call failed", e);
            answer = "抱歉，AI 服务暂时不可用，请稍后重试。"; // 降级消息，不阻断流程
        }

        // 防御：LLM 可能返回 null 或空串
        if (answer == null || answer.isBlank()) {
            answer = "抱歉，未能生成回复，请稍后重试。";
        }

        // —— 捕获 final 变量供 Lambda 使用 ——
        final String finalAnswer = answer;
        final Long sessionId = session.getId();
        final String toolMode = session.getMode(); // CHAT / KB_SEARCH

        // —— 异步 SSE 推送 ——
        // 启动新线程逐字推送，不阻塞主线程返回 emitter
        new Thread(() -> {
            // 手动恢复认证上下文——子线程不继承请求线程的 SecurityContext
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication(auth);
            try {
                // 逐字符发送 event:token 事件，每 5 字符暂停 5ms 控制打字节奏
                for (int i = 0; i < finalAnswer.length(); i++) {
                    String ch = finalAnswer.substring(i, i + 1);       // 每次取一个字符
                    emitter.send(SseEmitter.event().name("token").data(ch)); // 前端监听此事件追加字符
                    if (i % 5 == 0) Thread.sleep(5);                   // 控制推送速度
                }

                // —— 持久化 + 记忆更新（编程式事务，SSE 线程不受 @Transactional 覆盖） ——
                txTemplate.executeWithoutResult(status -> {
                    ChatMessage assistantMsg = new ChatMessage()
                            .setSessionId(sessionId)
                            .setRole("ASSISTANT")
                            .setContent(finalAnswer)
                            .setCreateTime(LocalDateTime.now());
                    messageMapper.insert(assistantMsg);

                    ChatSession s = sessionMapper.selectById(sessionId);
                    if (s != null) {
                        s.setMessageCount(s.getMessageCount() + 2);
                        sessionMapper.updateById(s);
                    }

                    memoryService.afterMessage(
                            sessionMapper.selectById(sessionId), toolMode, assistantMsg.getId());
                });

                // —— 发送流结束事件 ——
                // event:meta 携带 conversationId 和快捷追问建议
                emitter.send(SseEmitter.event().name("meta")
                        .data(Map.of("conversationId", sessionId)));
                emitter.complete();                                   // 通知客户端流正常结束
            } catch (Exception e) {
                log.error("SSE send failed", e);
                emitter.completeWithError(e);                         // 异常结束，客户端触发 onerror
            } finally {
                // 清理线程局部 SecurityContext，防止内存泄漏
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }).start(); // 启动线程后立即返回，不阻塞

        return emitter; // 主线程立即返回 emitter 给 Spring MVC
    }

    /**
     * 加载会话全部消息（按 id 升序 = 时间序）。
     * 用于 L3 运行时 token 估算——超阈值时截断只保留最近 N 条。
     *
     * @param sessionId 会话 ID
     * @return 按 id 升序排列的消息列表
     */
    private List<ChatMessage> loadRecentMessages(Long sessionId) {
        return messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)    // WHERE session_id = ?
                        .orderByAsc("id"));              // ORDER BY id ASC（自增主键 = 时间序）
    }

    /**
     * 获取或创建会话——对话入口的分发逻辑。
     * <p>
     * 请求带 sessionId 且存在 → 复用已有会话（继续对话）；
     * 否则 → 新建会话并同步创建 context 行（供记忆服务使用）。
     * 新会话标题取消息前 50 字符。
     * </p>
     *
     * @param request 前端请求（含 message、可选 sessionId、可选 mode）
     * @param staffId 当前登录员工 ID
     * @return 已存在或新建的会话对象（含自增主键）
     */
    private ChatSession getOrCreateSession(ChatRequest request, Integer staffId) {
        // 分支 1：复用已有会话
        if (request.getSessionId() != null) {
            ChatSession session = sessionMapper.selectById(request.getSessionId());
            if (session != null) return session; // 找到则直接返回
        }

        // 分支 2：新建会话
        ChatSession session = new ChatSession()
                .setStaffId(staffId)                                       // 归属当前员工
                .setTitle(request.getMessage() != null
                        ? request.getMessage().substring(0, Math.min(50,     // 截取前 50 字符
                                request.getMessage().length()))
                        : "新会话")
                .setMode(request.getMode() != null
                        ? request.getMode() : "CHAT")                      // 默认闲聊模式
                .setMessageCount(0)                                        // 初始消息数 0
                .setTotalTokens(0L)                                        // 初始 token 0
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);                                     // INSERT，回填自增主键

        // 同步创建会话上下文——一对一关系，session_id 即 context 主键
        ChatSessionContext ctx = new ChatSessionContext();
        ctx.setSessionId(session.getId());                                 // 与 session 表主键一致
        ctx.setContextVersion(0L);                                         // 乐观锁版本号从 0 开始
        ctx.setUpdateTime(LocalDateTime.now());
        contextMapper.insert(ctx);                                         // INSERT 初始行

        return session;
    }

    /**
     * 获取当前员工的历史会话列表，按更新时间倒序。
     * 限定当前员工 scope，不暴露他人会话。
     */
    public List<ChatSession> listSessions() {
        Integer staffId = securityUtil.getCurrentOperatorId();             // 从 JWT 提取
        return sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession>()
                        .eq("staff_id", staffId)                           // WHERE staff_id = ?
                        .orderByDesc("update_time"));                      // ORDER BY update_time DESC
    }

    /**
     * 按主键获取单个会话元数据。
     * 不做所有权校验——调用方应确保只传入当前员工的会话 ID。
     */
    public ChatSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /**
     * 获取会话消息历史——游标分页。
     * <p>
     * 利用降序索引 idx_msg_session(session_id ASC, create_time DESC)，
     * 查询走纯索引扫描，无需 filesort。
     * 默认返回最近 5 条，通过 before 游标加载更早消息。
     * </p>
     *
     * @param sessionId 会话 ID
     * @param before    游标——上一页首条消息的 create_time，null 表示首页
     * @param size      每页条数，默认 5，最大 50
     * @return { records: 消息列表(升序), hasMore: 是否有更多, nextCursor: 下页游标 }
     */
    public Map<String, Object> listMessages(Long sessionId, String before, int size) {
        int limit = Math.min(size, 50);                                    // 单次最多 50 条，防恶意请求
        var qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId);                              // WHERE session_id = ?

        // 游标条件：create_time < before，在降序索引中 = 更早的消息
        if (before != null) {
            qw.lt("create_time", before);                                  // WHERE create_time < ?
        }

        // ORDER BY create_time DESC——与索引方向一致，MySQL 直接按索引扫描，不走 filesort
        // 多取 1 条判断 hasMore
        qw.orderByDesc("create_time").last("LIMIT " + (limit + 1));

        List<ChatMessage> desc = messageMapper.selectList(qw);
        boolean hasMore = desc.size() > limit;                             // 多取的 1 条存在 → 还有更多
        if (hasMore) desc = desc.subList(0, limit);                        // 丢弃多取的那条

        java.util.Collections.reverse(desc);                               // 恢复升序（前端从上到下由旧到新）

        // nextCursor = 本页首条消息的 create_time（升序后 = desc[0]）
        String nextCursor = null;
        if (!desc.isEmpty()) {
            nextCursor = desc.get(0).getCreateTime() != null
                    ? desc.get(0).getCreateTime().toString() : null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", desc);                                       // 消息列表（升序）
        result.put("hasMore", hasMore);                                    // 是否有更多
        result.put("nextCursor", nextCursor);                              // 下页游标
        return result;
    }

    /** 合法模式白名单，防注入 */
    private static final Set<String> VALID_MODES = Set.of("CHAT", "KB_SEARCH");

    /**
     * 切换会话模式（CHAT ↔ KB_SEARCH）。
     * 仅校验合法性后更新 mode 字段——历史消息保留各自 toolMode 快照。
     */
    public void switchMode(Long sessionId, String mode) {
        if (mode != null && !VALID_MODES.contains(mode)) {                 // 白名单校验
            throw new IllegalArgumentException(
                    "不支持的模式: " + mode + "，有效值: CHAT, KB_SEARCH");
        }
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setMode(mode);
            sessionMapper.updateById(session);
        }
    }

    /**
     * 删除会话——包含级联清理。
     * <p>
     * 删除顺序：先消息（子表），再上下文（子表），最后会话（主表）。
     * 当前表无 FK 约束（InnoDB 外键高并发下有死锁风险），由应用层保证顺序。
     * </p>
     */
    @Transactional // 三条删除任一失败则全部回滚
    public void deleteSession(Long sessionId) {
        messageMapper.deleteBySessionId(sessionId);                        // DELETE FROM ast_chat_message WHERE session_id=?
        contextMapper.deleteById(sessionId);                               // DELETE FROM ast_chat_session_context WHERE session_id=?
        sessionMapper.deleteById(sessionId);                               // DELETE FROM ast_chat_session WHERE id=?
    }

    /**
     * 加载前端展示用会话摘要。
     * 非 LLM 摘要——纯字符串拼接截断，用于会话列表页概览展示。
     *
     * @param sessionId     会话 ID
     * @param lastMessageAt 最后消息时间，用于判断摘要是否过期
     * @return 摘要文本，无可复用摘要时返回 null
     */
    public String loadSummaryText(Long sessionId, LocalDateTime lastMessageAt) {
        return summaryService.loadReusableSummary(sessionId, lastMessageAt);
    }
}
