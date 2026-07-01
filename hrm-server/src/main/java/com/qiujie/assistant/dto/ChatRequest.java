package com.qiujie.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public class ChatRequest {

    private Long sessionId;
    private String message;
    private String mode;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    @JsonProperty("conversationId")
    public void setConversationId(Long conversationId) { this.sessionId = conversationId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    private static final Set<String> VALID_MODES = Set.of("CHAT", "KB_SEARCH");

    public String getMode() { return mode; }
    public void setMode(String mode) {
        if (mode != null && !VALID_MODES.contains(mode)) {
            throw new IllegalArgumentException("不支持的模式: " + mode + "，有效值: CHAT, KB_SEARCH");
        }
        this.mode = mode;
    }

    @JsonProperty("scene")
    public void setScene(String scene) {
        this.mode = "KB_SEARCH".equals(scene) ? "KB_SEARCH" : "CHAT";
    }
}
