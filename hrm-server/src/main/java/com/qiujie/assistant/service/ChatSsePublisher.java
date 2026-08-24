package com.qiujie.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE 流式推送器——异步逐字推送 + 回复持久化 + 记忆触发。
 * <p>
 * 从 {@link ChatService} 拆出的 SSE 协议关注点：字符级推送、SecurityContext 跨线程传递。
 * 回复持久化与记忆更新委托给 {@link ConversationContext#recordResponse(String, String)}，
 * 本层只管"推送 + 触发记录"。
 * </p>
 */
@Component
public class ChatSsePublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatSsePublisher.class);

    /** SSE 超时：5 分钟 */
    private static final long SSE_TIMEOUT = 300_000L;

    private final ThreadPoolTaskExecutor fileTaskExecutor;

    public ChatSsePublisher(ThreadPoolTaskExecutor fileTaskExecutor) {
        this.fileTaskExecutor = fileTaskExecutor;
    }

    /** 创建 SSE 发射器（5 分钟超时）。 */
    public SseEmitter newEmitter() {
        return new SseEmitter(SSE_TIMEOUT);
    }

    /**
     * 异步推送回答 + 持久化 + 记忆更新。
     * 在 {@code fileTaskExecutor} 线程池上执行，不阻塞主线程。
     *
     * @param emitter     SSE 发射器
     * @param finalAnswer LLM 完整回答文本
     * @param sessionId   会话 ID
     * @param auth        请求线程的认证上下文（跨线程恢复用）
     */
    public void push(SseEmitter emitter, String finalAnswer,
                     Long sessionId, Authentication auth) {
        fileTaskExecutor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                // 逐字符推送，每 5 字符暂停 5ms 控制打字节奏
                for (int i = 0; i < finalAnswer.length(); i++) {
                    String ch = finalAnswer.substring(i, i + 1);
                    emitter.send(SseEmitter.event().name("token").data(ch));
                    if (i % 5 == 0) Thread.sleep(5);
                }

                // 流结束事件
                emitter.send(SseEmitter.event().name("meta")
                        .data(Map.of("conversationId", sessionId)));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE send failed: sessionId={}", sessionId, e);
                emitter.completeWithError(e);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }
}