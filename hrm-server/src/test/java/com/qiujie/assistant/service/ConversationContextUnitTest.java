package com.qiujie.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.entity.ChatSessionContext;
import com.qiujie.assistant.mapper.ChatMessageMapper;
import com.qiujie.assistant.mapper.ChatSessionContextMapper;
import com.qiujie.assistant.mapper.ChatSessionMapper;
import com.qiujie.assistant.memory.ChatMemoryProperties;
import com.qiujie.assistant.memory.ChatMemorySummarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ConversationContext} 边界测试。
 * <p>
 * 测试聚合根公共 API（prepareLlmContext / recordResponse / displaySummary）的行为，
 * 不测试私有方法。所有 Mapper 依赖用 Mockito mock。
 * </p>
 */
@DisplayName("ConversationContext 聚合根行为")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationContextUnitTest {

    @Mock
    private ChatSessionMapper sessionMapper;
    @Mock
    private ChatMessageMapper messageMapper;
    @Mock
    private ChatSessionContextMapper contextMapper;
    @Mock
    private ChatMemorySummarizer summarizer;

    private ChatMemoryProperties props;
    private ChatSession session;
    private ChatSessionContext ctx;

    @BeforeEach
    void setUp() {
        props = new ChatMemoryProperties();
        props.setL1MessageTrigger(4);
        props.setL1TokenTrigger(1200);
        props.setL2MessageTrigger(6);
        props.setL2TokenTrigger(1800);
        props.setMaxTokens(50000);
        props.setKeepRecent(3);
        props.setCompactSummaryMaxTokens(2400);

        session = new ChatSession()
                .setId(1L)
                .setStaffId(100)
                .setMode("CHAT")
                .setMessageCount(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());

        ctx = new ChatSessionContext();
        ctx.setSessionId(1L);
        ctx.setContextVersion(0L);
        ctx.setUpdateTime(LocalDateTime.now());
    }

    private ConversationContext createContext() {
        return new ConversationContext(session, sessionMapper, messageMapper,
                contextMapper, summarizer, props);
    }

    // ==================== prepareLlmContext ====================

    @Test
    @DisplayName("prepareLlmContext: 无记忆时返回空 systemContext")
    void prepareLlmContext_noMemory() {
        when(contextMapper.selectById(1L)).thenReturn(null);
        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

        ConversationContext ctx2 = createContext();
        LlmContext llmCtx = ctx2.prepareLlmContext("你好");

        assertNull(llmCtx.systemContext());
        assertTrue(llmCtx.historyMessages().isEmpty());
        assertFalse(llmCtx.hasMemory());
    }

    @Test
    @DisplayName("prepareLlmContext: 有 L1+L2 记忆时返回合并的 systemContext")
    void prepareLlmContext_withMemory() {
        ctx.setSessionMemory("用户提到想请假");
        ctx.setCompactSummary("历史对话：讨论了请假政策");
        when(contextMapper.selectById(1L)).thenReturn(ctx);
        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

        ConversationContext ctx2 = createContext();
        LlmContext llmCtx = ctx2.prepareLlmContext("请帮我请假");

        assertTrue(llmCtx.hasMemory());
        assertTrue(llmCtx.systemContext().contains("历史对话精要"));
        assertTrue(llmCtx.systemContext().contains("当前会话关键信息"));
        // 有记忆时清空历史消息
        assertTrue(llmCtx.historyMessages().isEmpty());
    }

    @Test
    @DisplayName("prepareLlmContext: 无 L1/L2 但有历史消息时保留历史")
    void prepareLlmContext_historyWithoutMemory() {
        when(contextMapper.selectById(1L)).thenReturn(null);

        ChatMessage userMsg = new ChatMessage()
                .setId(1L).setSessionId(1L).setRole("USER").setContent("你好");
        ChatMessage asstMsg = new ChatMessage()
                .setId(2L).setSessionId(1L).setRole("ASSISTANT").setContent("你好！有什么可以帮助你的？");
        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(userMsg, asstMsg));

        ConversationContext ctx2 = createContext();
        LlmContext llmCtx = ctx2.prepareLlmContext("你好");

        assertNull(llmCtx.systemContext());
        assertEquals(2, llmCtx.historyMessages().size());
        assertInstanceOf(UserMessage.class, llmCtx.historyMessages().get(0));
        assertInstanceOf(AssistantMessage.class, llmCtx.historyMessages().get(1));
    }

    // ==================== recordResponse ====================

    @Test
    @DisplayName("recordResponse: 持久化助手消息 + 更新会话统计")
    void recordResponse_persistsMessage() {
        // maintain 中需要 selectList 返回空列表，否则空指针
        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());
        when(contextMapper.selectById(1L)).thenReturn(ctx);

        ConversationContext ctx2 = createContext();
        ctx2.recordResponse("这是回答", "CHAT");

        // 验证助手消息已持久化（insert(T) 单参重载）
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageMapper).insert(captor.capture());
        ChatMessage persisted = captor.getValue();
        assertEquals("ASSISTANT", persisted.getRole());
        assertEquals("这是回答", persisted.getContent());

        // 验证会话计数 +2
        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionMapper).updateById(sessionCaptor.capture());
        assertEquals(2, sessionCaptor.getValue().getMessageCount());
    }

    @Test
    @DisplayName("recordResponse: 无任何消息时静默返回（防御）")
    void recordResponse_emptyMessages() {
        // maintain 中需要 selectList 返回空列表，否则空指针
        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());
        when(contextMapper.selectById(1L)).thenReturn(ctx);

        ConversationContext ctx2 = createContext();
        ctx2.recordResponse("这是回答", "CHAT");

        verify(summarizer, never()).summarizeSessionMemory(any(), anyList(), anyString());
    }

    @Test
    @DisplayName("recordResponse: 消息数触发 L1 压缩时调 summarizer")
    void recordResponse_triggersL1() {
        // 5 条已有消息 + 本轮新消息 → 触发 L1（阈值 4）
        when(messageMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(createMessages(5, 50));
        when(contextMapper.selectById(1L)).thenReturn(ctx);
        when(summarizer.summarizeSessionMemory(any(), anyList(), anyString()))
                .thenReturn("更新后的记忆");

        ConversationContext ctx2 = createContext();
        ctx2.recordResponse("这是回答", "CHAT");

        verify(summarizer).summarizeSessionMemory(any(), anyList(), eq("CHAT"));
        // CAS 乐观锁写入（update 被调用）
        verify(contextMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("recordResponse: 未达 L1 阈值时不调 summarizer")
    void recordResponse_noTrigger() {
        // 2 条短消息，不满足 L1 阈值
        when(messageMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(createMessages(2, 50));
        when(contextMapper.selectById(1L)).thenReturn(ctx);

        ConversationContext ctx2 = createContext();
        ctx2.recordResponse("这是回答", "CHAT");

        verify(summarizer, never()).summarizeSessionMemory(any(), anyList(), anyString());
    }

    // ==================== displaySummary ====================

    @Test
    @DisplayName("displaySummary: 有摘要文本时返回")
    void displaySummary_withText() {
        ctx.setSummaryText("用户咨询了请假政策");
        when(contextMapper.selectById(1L)).thenReturn(ctx);

        ConversationContext ctx2 = createContext();
        assertEquals("用户咨询了请假政策", ctx2.displaySummary());
    }

    @Test
    @DisplayName("displaySummary: 无摘要文本时返回 null")
    void displaySummary_null() {
        when(contextMapper.selectById(1L)).thenReturn(ctx);

        ConversationContext ctx2 = createContext();
        assertNull(ctx2.displaySummary());
    }

    @Test
    @DisplayName("displaySummary: 上下文不存在时返回 null")
    void displaySummary_noContext() {
        when(contextMapper.selectById(1L)).thenReturn(null);

        ConversationContext ctx2 = createContext();
        assertNull(ctx2.displaySummary());
    }

    // ==================== helpers ====================

    private List<ChatMessage> createMessages(int count, int charPerMessage) {
        String content = "x".repeat(charPerMessage);
        java.util.ArrayList<ChatMessage> list = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            ChatMessage m = new ChatMessage()
                    .setId((long) i + 1)
                    .setSessionId(1L)
                    .setRole(i % 2 == 0 ? "USER" : "ASSISTANT")
                    .setContent(content)
                    .setCreateTime(LocalDateTime.now());
            list.add(m);
        }
        return list;
    }
}