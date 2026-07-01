package com.qiujie.assistant.memory;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Prompt 模板配置，注册三个 StringTemplate Bean，供记忆压缩和会话摘要使用。
 *
 * @author qiujie
 */
@Configuration
public class ChatMemoryPromptConfig {

    @Bean
    @Qualifier("assistantSessionMemoryPromptTemplate")
    public PromptTemplate assistantSessionMemoryPromptTemplate() {
        return PromptTemplate.builder()
                .resource(new ClassPathResource("prompts/assistant/session-memory-update.st"))
                .build();
    }

    @Bean
    @Qualifier("assistantCompactSummaryPromptTemplate")
    public PromptTemplate assistantCompactSummaryPromptTemplate() {
        return PromptTemplate.builder()
                .resource(new ClassPathResource("prompts/assistant/session-compact-summary.st"))
                .build();
    }

    @Bean
    @Qualifier("assistantRuntimeCompactPromptTemplate")
    public PromptTemplate assistantRuntimeCompactPromptTemplate() {
        return PromptTemplate.builder()
                .resource(new ClassPathResource("prompts/assistant/runtime-compact-summary.st"))
                .build();
    }
}
