package com.qiujie.knowledge.service;

import com.qiujie.knowledge.service.EvidenceAssessmentService.Assessment;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * RAG 问答 Prompt 模板——从 .st 文件加载，与 {@link com.qiujie.assistant.memory.ChatMemoryPromptConfig} 一致。
 * <p>
 * 从 {@link QaService} 拆出的 Prompt 构建关注点，模板外部化为
 * {@code prompts/knowledge/answer-prompt.st}，修改 Prompt 不再需要改 Java 源码。
 * </p>
 */
@Component
public class RAGPrompts {

    private final PromptTemplate answerPromptTemplate;

    public RAGPrompts(
            @Qualifier("knowledgeAnswerPromptTemplate") PromptTemplate answerPromptTemplate) {
        this.answerPromptTemplate = answerPromptTemplate;
    }

    /**
     * 构建回答 Prompt。
     *
     * @param question   用户问题
     * @param context    参考资料上下文（含引用编号）
     * @param assessment 证据充分度评估
     * @return 格式化后的 Prompt 文本
     */
    public String buildAnswerPrompt(String question, String context, Assessment assessment) {
        return answerPromptTemplate.render(java.util.Map.of(
                "evidenceLevel", assessment.level().name(),
                "evidenceReason", assessment.reason(),
                "context", context
        ));
    }
}