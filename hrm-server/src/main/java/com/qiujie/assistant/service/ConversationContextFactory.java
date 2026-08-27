package com.qiujie.assistant.service;

import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.mapper.ChatMessageMapper;
import com.qiujie.assistant.mapper.ChatSessionContextMapper;
import com.qiujie.assistant.mapper.ChatSessionMapper;
import com.qiujie.assistant.memory.ChatMemoryProperties;
import com.qiujie.assistant.memory.ChatMemorySummarizer;
import com.qiujie.assistant.store.ChatSessionStore;
import com.qiujie.util.SecurityUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 对话上下文工厂——{@link ConversationContext} 的唯一创建入口。
 * <p>
 * 职责：
 * <ul>
 *   <li>获取或创建会话（委托 {@link ChatSessionStore#openOrCreate}——创建编排收敛到存储端口）</li>
 *   <li>自动持久化用户消息（调用方不再需要手动 insertMessage）</li>
 *   <li>构建并返回已就绪的 {@link ConversationContext} 聚合根</li>
 * </ul>
 * </p>
 *
 * <p>
 * 会话/上下文/消息三表编排已下沉到 {@link ChatSessionStore}，本工厂只负责
 * 「组装聚合根」与「用户消息先落库」两步，是聚合根与 Spring 容器之间的桥梁。
 * </p>
 */
@Component
public class ConversationContextFactory {

    private final ChatSessionStore sessionStore;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionContextMapper contextMapper;
    private final ChatMemorySummarizer summarizer;
    private final ChatMemoryProperties props;
    private final SecurityUtil securityUtil;

    public ConversationContextFactory(ChatSessionStore sessionStore,
                                      ChatSessionMapper sessionMapper,
                                      ChatMessageMapper messageMapper,
                                      ChatSessionContextMapper contextMapper,
                                      ChatMemorySummarizer summarizer,
                                      ChatMemoryProperties props,
                                      SecurityUtil securityUtil) {
        this.sessionStore = sessionStore;
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
     *   <li>通过 {@link ChatSessionStore#openOrCreate} 获取/创建会话（含一对一 context 行）</li>
     *   <li>持久化用户消息（先于 LLM 调用写入，即使 LLM 超时也不丢失）</li>
     *   <li>返回已就绪的 {@link ConversationContext}</li>
     * </ol>
     * </p>
     *
     * @param sessionId 可选，已有会话 ID
     * @param message   用户消息（新会话时用作标题）
     * @param mode      会话模式 CHAT / KB_SEARCH
     * @return 已就绪的对话上下文（用户消息已持久化）
     */
    public ConversationContext getOrCreate(Long sessionId, String message, String mode) {
        Integer staffId = securityUtil.getCurrentOperatorId();

        // 1. 获取/创建会话（openOrCreate 内部完成会话 + context 行的创建）
        ChatSession session = sessionStore.openOrCreate(sessionId, message, mode, staffId);

        // 2. 持久化用户消息（会话已就绪，先落库再组装）
        insertUserMessage(session.getId(), message);

        // 3. 组装聚合根（聚合根内部的记忆维护仍直连 Mapper，见 ConversationContext）
        return new ConversationContext(session, sessionMapper, messageMapper,
                contextMapper, summarizer, props);
    }

    private void insertUserMessage(Long sessionId, String message) {
        ChatMessage msg = new ChatMessage()
                .setSessionId(sessionId)
                .setRole("USER")
                .setContent(message)
                .setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }
}