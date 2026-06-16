package com.qiujie.knowledge.event;

/**
 * 文档摄入事件。文档上传完成后发布，由异步监听器消费。
 */
public class DocumentIngestionEvent {

    private final Long documentId;

    public DocumentIngestionEvent(Long documentId) {
        this.documentId = documentId;
    }

    public Long getDocumentId() {
        return documentId;
    }
}
