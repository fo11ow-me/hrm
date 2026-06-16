package com.qiujie.assistant;

import com.qiujie.common.llm.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleAssistantLlmClient implements AssistantLlmClient, LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAssistantLlmClient.class);

    private final ChatClient chatClient;

    public OpenAiCompatibleAssistantLlmClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generate(String systemPrompt, String userInput) {
        try {
            String prompt = systemPrompt != null && !systemPrompt.isBlank()
                    ? systemPrompt + "\n\n" + userInput
                    : userInput;
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            return "抱歉，AI 服务暂时不可用，请稍后重试。";
        }
    }
}
