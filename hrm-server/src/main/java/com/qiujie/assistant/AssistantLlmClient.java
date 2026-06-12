package com.qiujie.assistant;

import java.util.List;
import java.util.Map;

public interface AssistantLlmClient {

    String generate(String question, String toolContext);

    default String generate(String question, String toolContext, List<Map<String, String>> history) {
        return generate(question, toolContext);
    }
}
