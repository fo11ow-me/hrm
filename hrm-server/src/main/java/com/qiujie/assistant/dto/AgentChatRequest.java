package com.qiujie.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentChatRequest {

    private Long sessionId;
    private String message;
    private String mode;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    @JsonProperty("conversationId")
    public void setConversationId(Long conversationId) { this.sessionId = conversationId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    @JsonProperty("scene")
    public void setScene(String scene) {
        this.mode = "KB_SEARCH".equals(scene) ? "KB_SEARCH" : "CHAT";
    }
}
