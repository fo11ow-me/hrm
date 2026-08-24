package com.qiujie.knowledge.service;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * 知识库 RAG 问答 Prompt 模板配置。
 * 注册 {@code knowledgeAnswerPromptTemplate} Bean，供 {@link RAGPrompts} 使用。
 */
@Configuration
public class KnowledgePromptConfig {

    @Bean
    @Qualifier("knowledgeAnswerPromptTemplate")
    public PromptTemplate knowledgeAnswerPromptTemplate() {
        return PromptTemplate.builder()
                .resource(new ClassPathResource("prompts/knowledge/answer-prompt.st"))
                .build();
    }
}