package com.qiujie.assistant.memory;

import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.llm.AssistantLlm;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LLM 驱动的记忆摘要生成器，提供 L1/L2 两级摘要能力。
 * <ul>
 *   <li>L1 — 会话记忆更新（session memory update）</li>
 *   <li>L2 — 紧凑摘要生成（compact summary）</li>
 * </ul>
 * <p>
 * L3 运行时上下文压缩（runtime compact）已于 2026-08 移除——该能力从未被
 * 调用（零调用方，仅定义于 Prompt 配置），运行时截断统一由
 * {@code ConversationContext.truncateIfNeeded()} 完成。
 * </p>
 *
 * @author qiujie
 */
@Component
public class ChatMemorySummarizer {

    private final AssistantLlm llm;
    private final PromptTemplate sessionMemoryPromptTemplate;
    private final PromptTemplate compactSummaryPromptTemplate;

    /**
     * 构造注入 LLM 边界与两个 PromptTemplate Bean。
     *
     * @param llm                         LLM 调用边界
     * @param sessionMemoryPromptTemplate L1 会话记忆更新模板
     * @param compactSummaryPromptTemplate L2 紧凑摘要模板
     */
    public ChatMemorySummarizer(
            AssistantLlm llm,
            @Qualifier("assistantSessionMemoryPromptTemplate") PromptTemplate sessionMemoryPromptTemplate,
            @Qualifier("assistantCompactSummaryPromptTemplate") PromptTemplate compactSummaryPromptTemplate) {
        this.llm = llm;
        this.sessionMemoryPromptTemplate = sessionMemoryPromptTemplate;
        this.compactSummaryPromptTemplate = compactSummaryPromptTemplate;
    }

    /**
     * L1: 将会话记忆与新增消息合并产生新的会话记忆摘要。
     *
     * @param existingSessionMemory 已有的会话记忆（可能为空）
     * @param newMessages           本轮新增的消息列表
     * @param toolMode              当前工具模式
     * @return 新的会话记忆摘要
     */
    public String summarizeSessionMemory(
            String existingSessionMemory, List<ChatMessage> newMessages, String toolMode) {
        return llm.summarize(sessionMemoryPromptTemplate.create(Map.of(
                "existingSessionMemory", defaultText(existingSessionMemory),
                "newMessages", formatMessages(newMessages),
                "currentToolMode", defaultText(toolMode),
                "currentGroupId", "NONE"
        )));
    }

    /**
     * L2: FIFO 追加模式——从本轮新消息独立提取要点，追加到已有摘要尾部。
     * 不读 L1，不从 L1 派生，独立信息源。
     *
     * @param existingCompactSummary 已有的紧凑摘要全文（可能为空）
     * @param newMessages            本轮新增的原始消息列表
     * @param toolMode               当前工具模式
     * @return 本轮需追加的新要点，无值得记忆的内容时返回 "NONE"
     */
    public String summarizeCompactSummary(
            String existingCompactSummary, List<ChatMessage> newMessages, String toolMode) {
        return llm.summarize(compactSummaryPromptTemplate.create(Map.of(
                "existingCompactSummary", defaultText(existingCompactSummary),
                "newMessages", formatMessages(newMessages),
                "currentToolMode", defaultText(toolMode)
        )));
    }

    /**
     * 将消息列表格式化为 {@code [role] content} 形式的文本，每行一条。
     *
     * @param messages 消息列表
     * @return 格式化文本，无消息时返回 "NONE"
     */
    String formatMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "NONE";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages) {
            if (m == null || m.getContent() == null || m.getContent().isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(System.lineSeparator());
            }
            sb.append('[').append(defaultText(m.getRole())).append("] ").append(m.getContent().trim());
        }
        return sb.isEmpty() ? "NONE" : sb.toString();
    }

    /**
     * 将空或空白字符串转换为 "NONE"，否则 trim 后返回。
     */
    private String defaultText(String value) {
        return value == null || value.isBlank() ? "NONE" : value.trim();
    }
}
