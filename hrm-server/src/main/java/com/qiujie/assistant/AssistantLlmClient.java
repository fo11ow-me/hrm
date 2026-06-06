package com.qiujie.assistant;

public interface AssistantLlmClient {

    String generate(String question, String toolContext);
}
