package com.qiujie.knowledge.event;

import com.qiujie.knowledge.service.DocumentIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文档摄入异步监听器。
 * 在事务提交后异步执行，不阻塞上传响应。
 */
@Component
public class DocumentIngestionListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionListener.class);

    @Autowired
    private DocumentIngestionService ingestionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentIngestionRequested(DocumentIngestionEvent event) {
        log.info("ETL started: documentId={}", event.getDocumentId());
        try {
            ingestionService.ingest(event.getDocumentId());
            log.info("ETL completed: documentId={}", event.getDocumentId());
        } catch (Exception e) {
            log.error("ETL failed: documentId={}", event.getDocumentId(), e);
        }
    }
}
