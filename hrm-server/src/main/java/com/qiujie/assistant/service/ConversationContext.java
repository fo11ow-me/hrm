package com.qiujie.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.entity.ChatSessionContext;
import com.qiujie.assistant.mapper.ChatMessageMapper;
import com.qiujie.assistant.mapper.ChatSessionContextMapper;
import com.qiujie.assistant.mapper.ChatSessionMapper;
import com.qiujie.assistant.memory.ChatMemoryProperties;
import com.qiujie.assistant.memory.ChatMemorySummarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话上下文——会话记忆的聚合根。
 * <p>
 * 封装"一段对话"的全部记忆行为：消息持久化、L1/L2/L3 三级记忆压缩、
 * 乐观锁 CAS 写入、token 估算与阈值判断。
 * 调用方（{@link ChatService}）只需知道"准备 LLM 上下文"和"记录回复"两步，
 * 记忆如何管理完全隐藏在内。
 * </p>
 *
 * <p>
 * 本类不是 Spring Bean——每个活跃会话对应一个实例，由 {@link ConversationContextFactory} 创建。
 * 实例可安全地在单轮请求中持有，不跨请求共享。
 * </p>
 */
public final class ConversationContext {

    private static final Logger log = LoggerFactory.getLogger(ConversationContext.class);

    /** token 估算除数：中英文混合场景下约 2 字符 ≈ 1 token */
    private static final int TOKEN_DIVISOR = 2;

    private final ChatSession session;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionContextMapper contextMapper;
    private final ChatMemorySummarizer summarizer;
    private final ChatMemoryProperties props;

    /**
     * @param session        当前会话（已持久化，含主键 id）
     * @param sessionMapper  会话 Mapper
     * @param messageMapper  消息 Mapper
     * @param contextMapper  上下文 Mapper
     * @param summarizer     LLM 摘要生成器
     * @param props          记忆配置
     */
    ConversationContext(ChatSession session,
                        ChatSessionMapper sessionMapper,
                        ChatMessageMapper messageMapper,
                        ChatSessionContextMapper contextMapper,
                        ChatMemorySummarizer summarizer,
                        ChatMemoryProperties props) {
        this.session = session;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.contextMapper = contextMapper;
        this.summarizer = summarizer;
        this.props = props;
    }

    // ==================== 公共 API ====================

    /** 会话 ID（主键）。 */
    public Long sessionId() {
        return session.getId();
    }

    /** 会话模式（CHAT / KB_SEARCH）。 */
    public String mode() {
        return session.getMode();
    }

    /**
     * 准备 LLM 上下文——自动合并 L1/L2 记忆 + 历史消息截断。
     * <p>
     * 内部流程：
     * <ol>
     *   <li>从 {@code ast_chat_session_context} 加载 L1 会话记忆和 L2 紧凑摘要</li>
     *   <li>拼接为 system 段上下文</li>
     *   <li>加载全部历史消息，按 token 阈值截断（L3）</li>
     *   <li>当有记忆上下文时清空历史消息（记忆已覆盖全局背景，避免重复）</li>
     * </ol>
     * </p>
     *
     * @param userMessage 当前用户消息（仅用于截断后保留，不修改上下文）
     * @return 准备好的 LLM 上下文
     */
    public LlmContext prepareLlmContext(String userMessage) {
        // 1. 构建 L1/L2 记忆上下文
        ChatSessionContext ctx = contextMapper.selectById(session.getId());
        String systemContext = buildMemoryContext(ctx);

        // 2. 加载全部历史消息，按 token 截断（L3）
        List<ChatMessage> allMessages = messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", session.getId())
                        .orderByAsc("id"));

        List<ChatMessage> recent = truncateIfNeeded(allMessages);

        // 3. 转为 Spring AI Message 列表
        List<org.springframework.ai.chat.messages.Message> historyMessages = new ArrayList<>();
        for (ChatMessage m : recent) {
            if (m.getContent() == null || m.getContent().isBlank()) continue;
            if ("USER".equals(m.getRole())) {
                historyMessages.add(new UserMessage(m.getContent()));
            } else if ("ASSISTANT".equals(m.getRole())) {
                historyMessages.add(new AssistantMessage(m.getContent()));
            }
        }

        // 4. 有记忆上下文时清空历史消息（记忆已覆盖全局背景）
        boolean hasMemory = systemContext != null && !systemContext.isBlank();
        if (hasMemory) {
            historyMessages.clear();
        }

        return new LlmContext(systemContext, historyMessages);
    }

    /**
     * 记录回复——持久化助手消息 + 自动触发记忆更新（L1/L2）。
     * <p>
     * 内部流程：
     * <ol>
     *   <li>持久化助手消息到 {@code ast_chat_message}</li>
     *   <li>会话消息计数 +2</li>
     *   <li>判断是否满足 L1/L2 触发条件，满足则调用 LLM 摘要 + CAS 乐观锁写入</li>
     * </ol>
     * 调用方不需要调任何 afterMessage 方法。
     * </p>
     *
     * @param assistantMessage LLM 生成的回答文本
     * @param toolMode         当前工具模式（CHAT / KB_SEARCH）
     */
    public void recordResponse(String assistantMessage, String toolMode) {
        // 1. 持久化助手消息
        ChatMessage assistantMsg = new ChatMessage()
                .setSessionId(session.getId())
                .setRole("ASSISTANT")
                .setContent(assistantMessage)
                .setCreateTime(LocalDateTime.now());
        messageMapper.insert(assistantMsg);

        // 2. 更新会话统计
        session.setMessageCount(session.getMessageCount() != null
                ? session.getMessageCount() + 2 : 2);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 3. 触发记忆维护
        maintain(toolMode, assistantMsg.getId());
    }

    /**
     * 获取前端展示摘要（规则拼接，非 LLM）。
     * 若确认前端从未消费此字段，本方法可安全删除。
     *
     * @return 摘要文本，无内容或已过期时返回 {@code null}
     */
    @Nullable
    public String displaySummary() {
        ChatSessionContext ctx = contextMapper.selectById(session.getId());
        if (ctx == null || ctx.getSummaryText() == null || ctx.getSummaryText().isBlank()) {
            return null;
        }
        return ctx.getSummaryText();
    }

    // ==================== 私有：记忆上下文构建 ====================

    /**
     * 从 ChatSessionContext 构建 LLM 系统级记忆上下文。
     * 拼接顺序：L2 紧凑摘要（全局背景）→ L1 会话记忆（近期细节）。
     */
    @Nullable
    private String buildMemoryContext(@Nullable ChatSessionContext ctx) {
        if (ctx == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        if (ctx.getCompactSummary() != null && !ctx.getCompactSummary().isBlank()) {
            sb.append("【历史对话精要】\n")
                    .append(ctx.getCompactSummary())
                    .append("\n\n");
        }

        if (ctx.getSessionMemory() != null && !ctx.getSessionMemory().isBlank()) {
            sb.append("【当前会话关键信息】\n")
                    .append(ctx.getSessionMemory())
                    .append("\n\n");
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    // ==================== 私有：L3 截断 ====================

    /**
     * L3 运行时截断：按 token 估算阈值截断消息列表。
     * 超过 {@code maxTokens} 时只保留最近 {@code keepRecent} 条。
     */
    private List<ChatMessage> truncateIfNeeded(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        int estimatedTokens = estimateTokens(messages);
        if (estimatedTokens > props.getMaxTokens()) {
            int keep = props.getKeepRecent();
            List<ChatMessage> truncated = messages.subList(
                    Math.max(0, messages.size() - keep), messages.size());
            log.warn("Session {} L3 truncation: {} > {} tokens, kept {} messages",
                    session.getId(), estimatedTokens, props.getMaxTokens(), keep);
            return truncated;
        }
        return messages;
    }

    // ==================== 私有：L1/L2 记忆维护 ====================

    /** 记忆维护核心逻辑（从 ChatMemoryService 收编）。 */
    private void maintain(String toolMode, Long currentMessageId) {
        List<ChatMessage> allMessages = messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", session.getId())
                        .orderByAsc("id"));
        if (allMessages.isEmpty()) {
            return;
        }

        ChatSessionContext ctx = contextMapper.selectById(session.getId());
        long lastRangeEnd = ctx != null && ctx.getSessionMemoryRangeEndMessageId() != null
                ? ctx.getSessionMemoryRangeEndMessageId() : 0L;

        List<ChatMessage> newMessages = allMessages.stream()
                .filter(m -> m.getId() != null && m.getId() > lastRangeEnd)
                .collect(Collectors.toList());

        if (!shouldUpdateSessionMemory(newMessages)) {
            return;
        }

        if (ctx == null) {
            ctx = new ChatSessionContext();
            ctx.setSessionId(session.getId());
            ctx.setContextVersion(0L);
            ctx.setUpdateTime(LocalDateTime.now());
            contextMapper.insert(ctx);
            ctx = contextMapper.selectById(session.getId());
            if (ctx == null) return;
        }

        ChatSessionContext toWrite = ctx;
        toWrite.setSessionMemory(summarizer.summarizeSessionMemory(
                ctx.getSessionMemory(), newMessages, toolMode));
        toWrite.setSessionMemoryBaseMessageId(newMessages.get(0).getId());
        toWrite.setSessionMemoryRangeEndMessageId(
                newMessages.get(newMessages.size() - 1).getId());
        toWrite.setUpdateTime(LocalDateTime.now());

        int newTokens = estimateTokens(newMessages);

        long expectedVersion = ctx.getContextVersion() != null ? ctx.getContextVersion() : 0L;
        toWrite.setContextVersion(expectedVersion + 1);

        // L2
        if (shouldCompactSession(newMessages.size(), newTokens)) {
            String newChunk = summarizer.summarizeCompactSummary(
                    ctx.getCompactSummary(), newMessages, toolMode);
            if (!"NONE".equals(newChunk)) {
                String current = ctx.getCompactSummary() != null ? ctx.getCompactSummary() : "";
                String merged = current + "\n" + newChunk;

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

        // CAS 乐观锁写入（字段名版本控 CAS，避免 Lambda 元数据依赖）
        long writeVersion = expectedVersion;
        UpdateWrapper<ChatSessionContext> wrapper = new UpdateWrapper<>();
        wrapper.eq("session_id", session.getId());
        wrapper.eq("context_version", writeVersion);
        wrapper.set("session_memory", toWrite.getSessionMemory());
        wrapper.set("session_memory_base_message_id", toWrite.getSessionMemoryBaseMessageId());
        wrapper.set("session_memory_range_end_message_id", toWrite.getSessionMemoryRangeEndMessageId());
        wrapper.set("compact_summary", toWrite.getCompactSummary());
        wrapper.set("compact_summary_base_message_id", toWrite.getCompactSummaryBaseMessageId());
        wrapper.set("compact_summary_range_end_message_id", toWrite.getCompactSummaryRangeEndMessageId());
        wrapper.set("context_version", toWrite.getContextVersion());
        wrapper.set("update_time", toWrite.getUpdateTime());
        contextMapper.update(null, wrapper);
    }

    private boolean shouldUpdateSessionMemory(List<ChatMessage> newMessages) {
        if (newMessages.isEmpty()) return false;
        int tokens = estimateTokens(newMessages);
        return newMessages.size() >= props.getL1MessageTrigger()
                || tokens >= props.getL1TokenTrigger();
    }

    private boolean shouldCompactSession(int newMsgCount, int newTokens) {
        return newMsgCount >= props.getL2MessageTrigger()
                || newTokens >= props.getL2TokenTrigger();
    }

    // ==================== 私有：工具 ====================

    /** 估算消息列表 token 数（字符数 / 2）。 */
    private static int estimateTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return 0;
        int totalChars = messages.stream()
                .map(ChatMessage::getContent)
                .filter(c -> c != null && !c.isBlank())
                .mapToInt(String::length)
                .sum();
        return Math.max(1, totalChars / TOKEN_DIVISOR);
    }
}