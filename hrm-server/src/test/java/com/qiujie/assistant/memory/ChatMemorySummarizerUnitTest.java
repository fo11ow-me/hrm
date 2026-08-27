package com.qiujie.assistant.memory;

import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.llm.AssistantLlm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link ChatMemorySummarizer} 单元测试——验证 L1/L2 摘要委托 LLM 边界、
 * 模板渲染与降级透传契约（不 mock 模板，直接渲染真实 .st 资源）。
 */
@DisplayName("ChatMemorySummarizer L1/L2 摘要委托")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMemorySummarizerUnitTest {

    @Mock
    private AssistantLlm llm;

    private ChatMemorySummarizer summarizer;

    @BeforeEach
    void setUp() {
        PromptTemplate sessionTpl = PromptTemplate.builder()
                .resource(new org.springframework.core.io.ClassPathResource(
                        "prompts/assistant/session-memory-update.st"))
                .build();
        PromptTemplate compactTpl = PromptTemplate.builder()
                .resource(new org.springframework.core.io.ClassPathResource(
                        "prompts/assistant/session-compact-summary.st"))
                .build();
        summarizer = new ChatMemorySummarizer(llm, sessionTpl, compactTpl);
    }

    private ChatMessage msg(long id, String role, String content) {
        return new ChatMessage().setId(id).setRole(role).setContent(content);
    }

    // ==================== summarizeSessionMemory (L1) ====================

    @Test
    @DisplayName("summarizeSessionMemory：渲染模板并委托 llm.summarize")
    void l1_delegatesToLlm() {
        when(llm.summarize(any(Prompt.class))).thenReturn("新的会话记忆");

        String result = summarizer.summarizeSessionMemory(
                "旧记忆", List.of(msg(1L, "USER", "我想请假")), "CHAT");

        assertEquals("新的会话记忆", result);
        verify(llm).summarize(any(Prompt.class));
    }

    @Test
    @DisplayName("summarizeSessionMemory：模板渲染含消息格式化文本")
    void l1_rendersMessageTextIntoPrompt() {
        when(llm.summarize(any(Prompt.class))).thenAnswer(inv -> {
            Prompt p = inv.getArgument(0);
            String text = p.getContents();
            // 消息已格式化为 [role] content
            assertTrue(text.contains("[USER] 我想请假"));
            assertTrue(text.contains("旧记忆"));
            return "ok";
        });

        summarizer.summarizeSessionMemory("旧记忆",
                List.of(msg(1L, "USER", "我想请假")), "CHAT");

        verify(llm).summarize(any(Prompt.class));
    }

    // ==================== summarizeCompactSummary (L2) ====================

    @Test
    @DisplayName("summarizeCompactSummary：渲染模板并委托 llm.summarize")
    void l2_delegatesToLlm() {
        when(llm.summarize(any(Prompt.class))).thenReturn("NONE");

        String result = summarizer.summarizeCompactSummary(
                "已有摘要", List.of(msg(1L, "USER", "第二问")), "CHAT");

        assertEquals("NONE", result);
        verify(llm).summarize(any(Prompt.class));
    }

    // ==================== 降级透传 ====================

    @Test
    @DisplayName("LLM 返回空串 → 原样透传空串（降级语义由 llm 决定）")
    void delegatesEmptyResultVerbatim() {
        when(llm.summarize(any(Prompt.class))).thenReturn("");

        assertEquals("", summarizer.summarizeSessionMemory("旧", List.of(), "CHAT"));
    }

    // ==================== formatMessages ====================

    @Test
    @DisplayName("formatMessages：空列表 → NONE")
    void formatMessages_empty() {
        assertEquals("NONE", summarizer.formatMessages(List.of()));
        assertEquals("NONE", summarizer.formatMessages(null));
    }

    @Test
    @DisplayName("formatMessages：多角色消息 → [role] content 每行一条")
    void formatMessages_formats() {
        List<ChatMessage> msgs = List.of(
                msg(1L, "USER", " 你好 "),
                msg(2L, "ASSISTANT", "你好！")
        );
        String result = summarizer.formatMessages(msgs);
        assertTrue(result.contains("[USER] 你好"));
        assertTrue(result.contains("[ASSISTANT] 你好！"));
    }
}
