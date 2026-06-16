package com.qiujie.knowledge.enums;

/**
 * 知识库 QA 证据充分度。
 */
public enum EvidenceLevel {

    NONE("无相关证据"),
    WEAK("证据较弱"),
    PARTIAL("部分相关"),
    SUFFICIENT("充分证据");

    private final String description;

    EvidenceLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
