package com.qiujie.assistant.service;

import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * LLM 上下文——已准备好供 LLM 调用的输入参数。
 * <p>
 * 包含系统记忆上下文（L1/L2 合并后）和历史消息列表。
 * 由 {@link ConversationContext#prepareLlmContext(String)} 创建，
 * 调用方只需将此对象传给 ChatClient 即可发起 LLM 调用。
 * </p>
 *
 * @param systemContext  系统级记忆上下文（注入 prompt system 段），无记忆时为 {@code null}
 * @param historyMessages 历史消息列表（Spring AI Message 列表），已按 memory 策略截断/清空
 */
public record LlmContext(
        @Nullable String systemContext,
        List<Message> historyMessages
) {
    /** 是否有可用的记忆上下文。 */
    public boolean hasMemory() {
        return systemContext != null && !systemContext.isBlank();
    }
}