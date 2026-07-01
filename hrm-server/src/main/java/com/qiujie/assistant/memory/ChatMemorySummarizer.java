package com.qiujie.assistant.memory;

import com.qiujie.assistant.entity.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LLM 驱动的记忆摘要生成器，提供 L1/L2/L3 三级摘要能力。
 * <ul>
 *   <li>L1 — 会话记忆更新（session memory update）</li>
 *   <li>L2 — 紧凑摘要生成（compact summary）</li>
 *   <li>L3 — 运行时上下文压缩（runtime compact）</li>
 * </ul>
 *
 * @author qiujie
 */
@Component
public class ChatMemorySummarizer {

    private static final Logger log = LoggerFactory.getLogger(ChatMemorySummarizer.class);

    private final ChatClient chatClient;
    private final PromptTemplate sessionMemoryPromptTemplate;
    private final PromptTemplate compactSummaryPromptTemplate;
    private final PromptTemplate runtimeCompactPromptTemplate;

    /**
     * 构造注入所需的 ChatClient 和三个 PromptTemplate Bean。
     *
     * @param chatClientBuilder          ChatClient 构建器
     * @param sessionMemoryPromptTemplate L1 会话记忆更新模板
     * @param compactSummaryPromptTemplate L2 紧凑摘要模板
     * @param runtimeCompactPromptTemplate L3 运行时上下文压缩模板
     */
    public ChatMemorySummarizer(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("assistantSessionMemoryPromptTemplate") PromptTemplate sessionMemoryPromptTemplate,
            @Qualifier("assistantCompactSummaryPromptTemplate") PromptTemplate compactSummaryPromptTemplate,
            @Qualifier("assistantRuntimeCompactPromptTemplate") PromptTemplate runtimeCompactPromptTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.sessionMemoryPromptTemplate = sessionMemoryPromptTemplate;
        this.compactSummaryPromptTemplate = compactSummaryPromptTemplate;
        this.runtimeCompactPromptTemplate = runtimeCompactPromptTemplate;
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
        return callForText(sessionMemoryPromptTemplate.create(Map.of(
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
        return callForText(compactSummaryPromptTemplate.create(Map.of(
                "existingCompactSummary", defaultText(existingCompactSummary),
                "newMessages", formatMessages(newMessages),
                "currentToolMode", defaultText(toolMode)
        )));
    }

    /**
     * L3: 在运行时将紧凑摘要、会话记忆、最近消息与当前问题合并，生成轻量上下文。
     *
     * @param compactSummary  紧凑摘要
     * @param sessionMemory   会话记忆
     * @param recentMessages  最近消息文本
     * @param currentQuestion 当前用户问题
     * @return 运行时上下文文本
     */
    public String summarizeRuntimeContext(
            String compactSummary, String sessionMemory, String recentMessages, String currentQuestion) {
        return callForText(runtimeCompactPromptTemplate.create(Map.of(
                "compactSummary", defaultText(compactSummary),
                "sessionMemory", defaultText(sessionMemory),
                "recentMessages", defaultText(recentMessages),
                "currentQuestion", defaultText(currentQuestion)
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
     * 调用 LLM 生成文本。
     *
     * @param prompt Prompt 对象
     * @return LLM 返回的文本，失败时返回空字符串
     */
    private String callForText(Prompt prompt) {
        try {
            String content = chatClient.prompt(prompt).call().content();
            if (content == null || content.isBlank()) {
                log.warn("Memory summarizer returned empty content");
                return "";
            }
            return content.replace("\r\n", "\n").trim();
        } catch (Exception e) {
            log.warn("Memory summarizer call failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 将空或空白字符串转换为 "NONE"，否则 trim 后返回。
     */
    private String defaultText(String value) {
        return value == null || value.isBlank() ? "NONE" : value.trim();
    }
}
