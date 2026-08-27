package com.qiujie.assistant.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link AssistantLlm} 的生产实现——基于 Spring AI {@link ChatClient}。
 * <p>
 * 持有独立构建的 {@link ChatClient} 实例（与 {@code QaService} 隔离），
 * 并在内部统一处理「构建 → 调用 → 异常吞并降级」的边界逻辑：
 * </p>
 * <ul>
 *   <li>{@link #chat}：异常 → {@code null}；空白 → {@code null}（由调用方判定兜底）</li>
 *   <li>{@link #summarize}：异常 → {@code ""}；空白 → {@code ""}（静默降级）</li>
 * </ul>
 *
 * @author qiujie
 */
@Component
public class SpringAiAssistantLlm implements AssistantLlm {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAssistantLlm.class);

    private final ChatClient chatClient;

    public SpringAiAssistantLlm(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    @Nullable
    public String chat(List<Message> historyMessages, String userMessage,
                      @Nullable String systemContext, Object... tools) {
        try {
            var spec = chatClient.prompt()
                    .messages(historyMessages == null ? List.of() : historyMessages)
                    .user(userMessage)
                    .tools(tools);
            if (systemContext != null && !systemContext.isBlank()) {
                spec = spec.system(s -> s.text(systemContext));
            }
            String content = spec.call().content();
            return (content == null || content.isBlank()) ? null : content;
        } catch (Exception e) {
            log.error("Assistant LLM chat failed", e);
            return null;
        }
    }

    @Override
    public String summarize(Prompt prompt) {
        try {
            String content = chatClient.prompt(prompt).call().content();
            return (content == null || content.isBlank()) ? "" : content.replace("\r\n", "\n").trim();
        } catch (Exception e) {
            log.warn("Assistant LLM summarizer failed: {}", e.getMessage());
            return "";
        }
    }
}
