package com.qiujie.assistant.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link SpringAiAssistantLlm} 边界测试——验证降级语义契约：
 * <ul>
 *   <li>chat：成功透传；空白/异常 → null（由调用方判定兜底）</li>
 *   <li>summarize：成功 trim；空白/异常 → 空串（静默降级，不更新记忆）</li>
 * </ul>
 */
@DisplayName("SpringAiAssistantLlm 降级语义")
@ExtendWith(MockitoExtension.class)
class SpringAiAssistantLlmUnitTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClientRequestSpec requestSpec;
    @Mock
    private CallResponseSpec responseSpec;

    private SpringAiAssistantLlm llm;

    @BeforeEach
    void setUp() {
        llm = new SpringAiAssistantLlm(mock(ChatClient.Builder.class));
        // 直接注入 mock ChatClient（绕过 builder）
        injectChatClient();
    }

    private void injectChatClient() {
        try {
            java.lang.reflect.Field f = SpringAiAssistantLlm.class.getDeclaredField("chatClient");
            f.setAccessible(true);
            f.set(llm, chatClient);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ChatClientRequestSpec stubPrompt() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        return requestSpec;
    }

    // ==================== chat ====================

    @Test
    @DisplayName("chat：成功调用返回内容")
    void chat_returnsContent() {
        ChatClientRequestSpec spec = stubPrompt();
        when(spec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(" 你好 ");

        String result = llm.chat(List.of(new UserMessage("hi")), "在吗", null, new Object());

        assertEquals(" 你好 ", result);
        verify(requestSpec).tools(any());
    }

    @Test
    @DisplayName("chat：空白内容 → null")
    void chat_blankReturnsNull() {
        ChatClientRequestSpec spec = stubPrompt();
        when(spec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("   ");

        assertNull(llm.chat(List.of(), "在吗", null, new Object()));
    }

    @Test
    @DisplayName("chat：异常 → null（不抛出）")
    void chat_exceptionReturnsNull() {
        ChatClientRequestSpec spec = stubPrompt();
        when(spec.call()).thenThrow(new RuntimeException("upstream down"));

        assertNull(llm.chat(List.of(), "在吗", null, new Object()));
    }

    @Test
    @DisplayName("chat：有 system 上下文时注入 system 段")
    void chat_withSystemContext() {
        ChatClientRequestSpec spec = stubPrompt();
        when(spec.system(any(java.util.function.Consumer.class))).thenReturn(spec);
        when(spec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        String result = llm.chat(List.of(), "在吗", "【当前会话关键信息】测试", new Object());

        assertEquals("ok", result);
        verify(spec).system(any(java.util.function.Consumer.class));
    }

    // ==================== summarize ====================

    @Test
    @DisplayName("summarize：成功返回 trim 后的内容")
    void summarize_returnsTrimmed() {
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("\r\n要点\r\n  \r\n");

        String result = llm.summarize(new Prompt("prompt"));

        assertEquals("要点", result);
    }

    @Test
    @DisplayName("summarize：空白 → 空串")
    void summarize_blankReturnsEmpty() {
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(" \t ");

        assertEquals("", llm.summarize(new Prompt("prompt")));
    }

    @Test
    @DisplayName("summarize：异常 → 空串（不抛出）")
    void summarize_exceptionReturnsEmpty() {
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("timeout"));

        assertEquals("", llm.summarize(new Prompt("prompt")));
    }
}
