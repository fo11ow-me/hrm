package com.qiujie.assistant.service;

import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.entity.ChatSessionContext;
import com.qiujie.assistant.mapper.ChatMessageMapper;
import com.qiujie.assistant.mapper.ChatSessionContextMapper;
import com.qiujie.assistant.mapper.ChatSessionMapper;
import com.qiujie.assistant.memory.ChatMemoryProperties;
import com.qiujie.assistant.memory.ChatMemorySummarizer;
import com.qiujie.util.SecurityUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 对话上下文工厂——{@link ConversationContext} 的唯一创建入口。
 * <p>
 * 职责：
 * <ul>
 *   <li>获取或创建会话（复用 {@link ChatSessionService#getOrCreate} 的语义）</li>
 *   <li>自动持久化用户消息（调用方不再需要手动 {@code insertMessage}）</li>
 *   <li>构建并返回已就绪的 {@link ConversationContext} 聚合根</li>
 * </ul>
 * </p>
 *
 * <p>
 * 本工厂取代了 ChatService 中"getOrCreate → insertMessage"的手动编排，
 * 是 {@link ConversationContext} 聚合根与 Spring 容器之间的桥梁。
 * </p>
 */
@Component
public class ConversationContextFactory {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionContextMapper contextMapper;
    private final ChatMemorySummarizer summarizer;
    private final ChatMemoryProperties props;
    private final SecurityUtil securityUtil;

    public ConversationContextFactory(ChatSessionMapper sessionMapper,
                                      ChatMessageMapper messageMapper,
                                      ChatSessionContextMapper contextMapper,
                                      ChatMemorySummarizer summarizer,
                                      ChatMemoryProperties props,
                                      SecurityUtil securityUtil) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.contextMapper = contextMapper;
        this.summarizer = summarizer;
        this.props = props;
        this.securityUtil = securityUtil;
    }

    /**
     * 获取或创建会话上下文。
     * <p>
     * 流程：
     * <ol>
     *   <li>sessionId 不为 null 且存在 → 复用已有会话</li>
     *   <li>否则 → 新建会话 + 同步创建 context 行</li>
     *   <li>持久化用户消息（先于 LLM 调用写入，即使 LLM 超时也不丢失）</li>
     *   <li>返回已就绪的 {@link ConversationContext}</li>
     * </ol>
     * </p>
     *
     * @param sessionId   可选，已有会话 ID
     * @param message     用户消息（新会话时用作标题）
     * @param mode        会话模式 CHAT / KB_SEARCH
     * @return 已就绪的对话上下文（用户消息已持久化）
     */
    public ConversationContext getOrCreate(Long sessionId, String message, String mode) {
        Integer staffId = securityUtil.getCurrentOperatorId();

        // 分支 1：复用已有会话
        ChatSession session;
        if (sessionId != null) {
            session = sessionMapper.selectById(sessionId);
            if (session != null) {
                // 持久化用户消息
                insertUserMessage(session.getId(), message);
                return new ConversationContext(session, sessionMapper, messageMapper,
                        contextMapper, summarizer, props);
            }
        }

        // 分支 2：新建会话
        session = new ChatSession()
                .setStaffId(staffId)
                .setTitle(message != null && !message.isBlank()
                        ? message.substring(0, Math.min(50, message.length()))
                        : "新会话")
                .setMode(mode != null ? mode : "CHAT")
                .setMessageCount(0)
                .setTotalTokens(0L)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);

        // 一对一 context 行
        ChatSessionContext ctx = new ChatSessionContext();
        ctx.setSessionId(session.getId());
        ctx.setContextVersion(0L);
        ctx.setUpdateTime(LocalDateTime.now());
        contextMapper.insert(ctx);

        // 持久化用户消息
        insertUserMessage(session.getId(), message);

        return new ConversationContext(session, sessionMapper, messageMapper,
                contextMapper, summarizer, props);
    }

    private void insertUserMessage(Long sessionId, String message) {
        com.qiujie.assistant.entity.ChatMessage msg =
                new com.qiujie.assistant.entity.ChatMessage()
                        .setSessionId(sessionId)
                        .setRole("USER")
                        .setContent(message)
                        .setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }
}