package com.qiujie.knowledge.dto;

import java.util.List;

/**
 * 知识库问答响应
 */
public class QaResponse {

    private String answer;
    private String evidenceLevel;  // NONE / WEAK / PARTIAL / SUFFICIENT
    private String strategy;       // DIRECT / REWRITE / DECOMPOSE
    private List<CitationVO> citations;
    private String conversationId;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(String evidenceLevel) { this.evidenceLevel = evidenceLevel; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public List<CitationVO> getCitations() { return citations; }
    public void setCitations(List<CitationVO> citations) { this.citations = citations; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public static class CitationVO {
        private String documentName;
        private String chunkText;
        private double relevanceScore;

        public CitationVO() {}
        public CitationVO(String documentName, String chunkText, double relevanceScore) {
            this.documentName = documentName;
            this.chunkText = chunkText;
            this.relevanceScore = relevanceScore;
        }

        public String getDocumentName() { return documentName; }
        public void setDocumentName(String documentName) { this.documentName = documentName; }
        public String getChunkText() { return chunkText; }
        public void setChunkText(String chunkText) { this.chunkText = chunkText; }
        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
    }
}
