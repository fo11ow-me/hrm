package com.qiujie.assistant.service;

import com.qiujie.assistant.ChatTools;
import com.qiujie.assistant.dto.ChatRequest;
import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.llm.AssistantLlm;
import com.qiujie.assistant.store.ChatSessionStore;
import com.qiujie.util.SecurityUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 助手核心编排引擎。
 * <p>
 * 职责：管理对话生命周期（会话创建→消息持久化→LLM 调用→SSE 推送）。
 * 委托边界：
 * <ul>
 *   <li>会话 CRUD + 游标分页 → {@link ChatSessionStore}</li>
 *   <li>会话记忆（L1/L2/L3 压缩、token 截断）→ {@link ConversationContextFactory} + {@link ConversationContext}</li>
 *   <li>LLM 调用与 Prompt 构建 → {@link com.qiujie.assistant.llm.AssistantLlm}（领域端口）</li>
 *   <li>SSE 推送 → {@link ChatSsePublisher}</li>
 * </ul>
 * </p>
 *
 * @author quuj
 */
@Service
public class ChatService {

    private final ChatSessionStore sessionStore;
    private final ConversationContextFactory contextFactory;
    private final AssistantLlm llm;
    private final ChatTools chatTools;
    private final ChatSsePublisher ssePublisher;
    private final SecurityUtil securityUtil;

    public ChatService(ChatSessionStore sessionStore,
                       ConversationContextFactory contextFactory,
                       AssistantLlm llm,
                       ChatTools chatTools,
                       ChatSsePublisher ssePublisher,
                       SecurityUtil securityUtil) {
        this.sessionStore = sessionStore;
        this.contextFactory = contextFactory;
        this.llm = llm;
        this.chatTools = chatTools;
        this.ssePublisher = ssePublisher;
        this.securityUtil = securityUtil;
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

        // 3. 调用 LLM——失败降级语义在 {@link AssistantLlm} 内收口（返回 null）
        String answer = callLlm(request, llmCtx);

        // 4. 记录回复（先落库）→ 异步 SSE 推送（后推送）
        //    先落库后推送：回答与记忆先于推送持久化，避免事务回滚时 SSE 已发送
        //    （前端已收到内容但库里没有）的时序竞态。
        ctx.recordResponse(answer, ctx.mode());
        ssePublisher.push(emitter, answer, ctx.sessionId(), auth);

        return emitter;
    }

    // ==================== 查询（会话管理） ====================

    public List<ChatSession> listSessions() {
        return sessionStore.listSessions(currentStaffId());
    }

    public ChatSession getSession(Long sessionId) {
        return sessionStore.getById(sessionId);
    }

    public java.util.Map<String, Object> listMessages(Long sessionId, String before, int size) {
        return sessionStore.listMessages(sessionId, before, size);
    }

    public void switchMode(Long sessionId, String mode) {
        sessionStore.switchMode(sessionId, mode);
    }

    public void deleteSession(Long sessionId) {
        sessionStore.delete(sessionId);
    }

    /** 当前登录员工 ID（JWT），供会话查询范围限定。 */
    public Integer currentStaffId() {
        return securityUtil.getCurrentOperatorId();
    }

    // ==================== 私有 ====================

    /** 调用 LLM——失败/空白降级为兜底提示（不阻断流程）。 */
    private String callLlm(ChatRequest request, LlmContext llmCtx) {
        String answer = llm.chat(llmCtx.historyMessages(), request.getMessage(),
                llmCtx.systemContext(), chatTools);
        return (answer == null || answer.isBlank())
                ? "抱歉，AI 服务暂时不可用，请稍后重试。"
                : answer;
    }
}