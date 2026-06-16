package com.qiujie.common.llm;

/**
 * LLM 调用抽象。knowledge/ 和 assistant/ 都依赖此接口，而非具体实现。
 */
public interface LlmProvider {

    String generate(String systemPrompt, String userInput);

    default String generate(String prompt) {
        return generate(prompt, "");
    }
}
