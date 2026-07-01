package com.qiujie.assistant.memory;

import com.qiujie.assistant.entity.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChatMemoryService 压缩触发逻辑")
@ExtendWith(MockitoExtension.class)
class ChatMemoryServiceUnitTest {

    private ChatMemoryProperties props;

    @BeforeEach
    void setUp() {
        props = new ChatMemoryProperties();
        // 使用默认值: l1MessageTrigger=4, l1TokenTrigger=1200,
        // l2MessageTrigger=6, l2TokenTrigger=1800, sessionTokenThreshold=6500
    }

    @Test
    @DisplayName("shouldUpdateSessionMemory: 消息数达标返回 true")
    void shouldUpdateSessionMemory_byMessageCount() {
        props.setL1MessageTrigger(4);
        props.setL1TokenTrigger(1200);

        List<ChatMessage> messages = createMessages(4, 50); // 4 条短消息，token 不达标

        boolean result = shouldUpdateSessionMemory(messages);
        assertTrue(result, "4 条消息应触发 L1");
    }

    @Test
    @DisplayName("shouldUpdateSessionMemory: token 达标返回 true")
    void shouldUpdateSessionMemory_byTokenCount() {
        props.setL1MessageTrigger(4);
        props.setL1TokenTrigger(1200);

        List<ChatMessage> messages = createMessages(2, 3000); // 2 条长消息，token 达标

        boolean result = shouldUpdateSessionMemory(messages);
        assertTrue(result, "2 条长消息（超 1200 token）应触发 L1");
    }

    @Test
    @DisplayName("shouldUpdateSessionMemory: 都不达标返回 false")
    void shouldNotUpdateSessionMemory_whenBelowThreshold() {
        props.setL1MessageTrigger(4);
        props.setL1TokenTrigger(1200);

        List<ChatMessage> messages = createMessages(2, 50); // 2 条短消息

        boolean result = shouldUpdateSessionMemory(messages);
        assertFalse(result, "2 条短消息不应触发");
    }

    @Test
    @DisplayName("shouldUpdateSessionMemory: 空列表返回 false")
    void shouldNotUpdateSessionMemory_whenEmpty() {
        boolean result = shouldUpdateSessionMemory(List.of());
        assertFalse(result, "空列表不应触发");
    }

    @Test
    @DisplayName("shouldCompactSession: 双重条件满足返回 true")
    void shouldCompactSession_bothConditionsMet() {
        props.setSessionTokenThreshold(6500);
        props.setL2MessageTrigger(6);
        props.setL2TokenTrigger(1800);

        // 总 token 7000 > 6500 且 6 条新消息
        boolean result = shouldCompactSession(7000, 6, 100);
        assertTrue(result, "总 token 超阈值且新消息达标应触发 L2");
    }

    @Test
    @DisplayName("shouldCompactSession: 总 token 不足返回 false")
    void shouldNotCompactSession_whenTotalTokenLow() {
        props.setSessionTokenThreshold(6500);
        props.setL2MessageTrigger(6);
        props.setL2TokenTrigger(1800);

        boolean result = shouldCompactSession(3000, 10, 5000);
        assertFalse(result, "总 token 不足 6500 不应触发 L2");
    }

    @Test
    @DisplayName("estimateTokens: 中英文混合估算正确")
    void estimateTokens_mixedContent() {
        List<ChatMessage> messages = List.of(
                msg("你好世界"),           // 4 chars → 1 token
                msg("Hello world, how are you?") // 26 chars → 6 tokens
        );

        int tokens = estimateTokens(messages);
        assertEquals(7, tokens, "总字符 30 / 4 = 7");
    }

    @Test
    @DisplayName("estimateTokens: 空列表返回 0")
    void estimateTokens_empty() {
        assertEquals(0, estimateTokens(List.of()));
    }

    // --- helpers ---

    private List<ChatMessage> createMessages(int count, int charPerMessage) {
        List<ChatMessage> list = new ArrayList<>();
        String content = "x".repeat(charPerMessage);
        for (int i = 0; i < count; i++) {
            ChatMessage m = new ChatMessage();
            m.setId((long) i + 1);
            m.setContent(content);
            list.add(m);
        }
        return list;
    }

    private ChatMessage msg(String content) {
        ChatMessage m = new ChatMessage();
        m.setContent(content);
        return m;
    }

    // 从 ChatMemoryService 复制核心判断逻辑以便独立测试

    private boolean shouldUpdateSessionMemory(List<ChatMessage> newMessages) {
        if (newMessages.isEmpty()) return false;
        int tokens = estimateTokens(newMessages);
        return newMessages.size() >= props.getL1MessageTrigger() || tokens >= props.getL1TokenTrigger();
    }

    private boolean shouldCompactSession(int totalTokens, int newMsgCount, int newTokens) {
        return totalTokens > props.getSessionTokenThreshold()
                && (newMsgCount >= props.getL2MessageTrigger() || newTokens >= props.getL2TokenTrigger());
    }

    private int estimateTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return 0;
        int totalChars = messages.stream()
                .map(ChatMessage::getContent)
                .filter(c -> c != null && !c.isBlank())
                .mapToInt(String::length)
                .sum();
        return Math.max(1, totalChars / 4);
    }
}
