package com.qiujie.assistant.service;

import com.qiujie.assistant.ChatTools;
import com.qiujie.assistant.dto.ChatRequest;
import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 助手核心编排引擎。
 * <p>
 * 职责：管理对话生命周期（会话创建→消息持久化→LLM 调用→SSE 推送）。
 * 委托边界：
 * <ul>
 *   <li>会话 CRUD + 游标分页 → {@link ChatSessionService}</li>
 *   <li>会话记忆（L1/L2/L3 压缩、token 截断）→ {@link ConversationContextFactory} + {@link ConversationContext}</li>
 *   <li>LLM 调用与 Prompt 构建 → 本类（内联）</li>
 *   <li>SSE 推送与回复落库 → {@link ChatSsePublisher}</li>
 * </ul>
 * </p>
 *
 * @author quuj
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionService sessionService;
    private final ConversationContextFactory contextFactory;
    private final ChatClient chatClient;
    private final ChatTools chatTools;
    private final ChatSsePublisher ssePublisher;

    public ChatService(ChatSessionService sessionService,
                       ConversationContextFactory contextFactory,
                       ChatClient.Builder chatClientBuilder,
                       ChatTools chatTools,
                       ChatSsePublisher ssePublisher) {
        this.sessionService = sessionService;
        this.contextFactory = contextFactory;
        this.chatClient = chatClientBuilder.build();
        this.chatTools = chatTools;
        this.ssePublisher = ssePublisher;
    }

    /**
     * SSE 流式对话——核心方法。
     * <p>
     * 完整流程：创建/复用会话 → 持久化用户消息 → 组装记忆 + L3 截断 → 调用 LLM →
     * 异步 SSE 推送 → 记录回复（持久化助手消息 + 触发记忆更新）。
     * </p>
     */
    @Transactional
    public SseEmitter chat(ChatRequest request) {
        SseEmitter emitter = ssePublisher.newEmitter();
        // 保存认证上下文——异步 SSE 推送线程不在请求线程内
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. 获取/创建会话 + 持久化用户消息（聚合根统一完成）
        ConversationContext ctx = contextFactory.getOrCreate(
                request.getSessionId(), request.getMessage(), request.getMode());

        // 2. 准备 LLM 上下文（L1/L2 记忆 + L3 截断，聚合根内部完成）
        LlmContext llmCtx = ctx.prepareLlmContext(request.getMessage());

        // 3. 调用 LLM
        String answer = callLlm(request, llmCtx);

        // 4. 异步 SSE 推送 + 落库（线程池执行，不阻塞主线程）
        ssePublisher.push(emitter, answer, ctx.sessionId(), auth);

        // 5. 记录回复——持久化助手消息 + 触发记忆更新
        ctx.recordResponse(answer, ctx.mode());
        return emitter;
    }

    // ==================== 查询（会话管理） ====================

    public List<ChatSession> listSessions() {
        return sessionService.listSessions(sessionService.currentStaffId());
    }

    public ChatSession getSession(Long sessionId) {
        return sessionService.getById(sessionId);
    }

    public java.util.Map<String, Object> listMessages(Long sessionId, String before, int size) {
        return sessionService.listMessages(sessionId, before, size);
    }

    public void switchMode(Long sessionId, String mode) {
        sessionService.switchMode(sessionId, mode);
    }

    public void deleteSession(Long sessionId) {
        sessionService.delete(sessionId);
    }

    // ==================== 私有 ====================

    /** 调用 LLM，失败时降级为兜底提示（不阻断流程）。 */
    private String callLlm(ChatRequest request, LlmContext llmCtx) {
        try {
            var prompt = chatClient.prompt()
                    .messages(llmCtx.historyMessages())
                    .user(request.getMessage())
                    .tools(chatTools);
            if (llmCtx.hasMemory()) {
                prompt = prompt.system(s -> s.text(llmCtx.systemContext()));
            }
            String answer = prompt.call().content();
            return (answer == null || answer.isBlank())
                    ? "抱歉，未能生成回复，请稍后重试。"
                    : answer;
        } catch (Exception e) {
            log.error("LLM call failed: sessionId={}", request.getSessionId(), e);
            return "抱歉，AI 服务暂时不可用，请稍后重试。";
        }
    }
}