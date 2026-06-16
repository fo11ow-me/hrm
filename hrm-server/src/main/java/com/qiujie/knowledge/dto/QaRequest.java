package com.qiujie.knowledge.dto;

/**
 * 知识库问答请求
 */
public class QaRequest {

    private String question;
    private String strategy; // DIRECT / REWRITE / DECOMPOSE / AUTO

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
}
