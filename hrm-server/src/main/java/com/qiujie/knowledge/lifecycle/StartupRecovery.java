package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

/**
 * 启动恢复（模块内部组件）：覆盖三个崩溃窗口。
 * <ol>
 *   <li>遗留 RUNNING 作业 → FAILED（收尾）</li>
 *   <li>执行中崩溃：PROCESSING → FAILED + 原因</li>
 *   <li>未启动崩溃：UPLOADED 续跑（CAS 认领兜底并发，语义优于置失败）</li>
 *   <li>已删孤儿重 purge（覆盖"逻辑删提交后、清理执行前"崩溃窗口，幂等）</li>
 * </ol>
 */
final class StartupRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRecovery.class);

    private static final String INTERRUPT_REASON = "文档处理因服务中断未完成，请重试";

    private final KnowledgeDocumentMapper documentMapper;
    private final IngestionJobMapper jobMapper;
    private final IngestionPipeline pipeline;
    private final DocumentPurgeHandler purgeHandler;

    StartupRecovery(KnowledgeDocumentMapper documentMapper,
                    IngestionJobMapper jobMapper,
                    IngestionPipeline pipeline,
                    DocumentPurgeHandler purgeHandler) {
        this.documentMapper = documentMapper;
        this.jobMapper = jobMapper;
        this.pipeline = pipeline;
        this.purgeHandler = purgeHandler;
    }

    @Override
    public void run(ApplicationArguments args) {
        int affected = recover();
        if (affected > 0) {
            log.info("Recovered {} documents on startup (failed/rerun/purged)", affected);
        }
    }

    /** 执行四步恢复，返回受影响（标失败/续跑/重清理）的文档数。 */
    int recover() {
        int affected = 0;

        // 1. 遗留 RUNNING 作业收尾
        jobMapper.markStaleRunningAsFailed(INTERRUPT_REASON);

        // 2. 执行中崩溃：PROCESSING → FAILED + 原因
        affected += documentMapper.markStaleProcessingAsFailed(INTERRUPT_REASON);

        // 3. 未启动崩溃：UPLOADED 续跑（ETL 从未开始；并发由 claim CAS 仲裁）
        List<KnowledgeDocument> uploaded = documentMapper.selectLiveUploaded();
        for (KnowledgeDocument doc : uploaded) {
            pipeline.run(doc.getId());
            affected++;
        }

        // 4. 已删孤儿重 purge（幂等）
        List<KnowledgeDocument> deleted = documentMapper.selectDeleted();
        for (KnowledgeDocument doc : deleted) {
            purgeHandler.purge(doc.getId(), doc.getName());
            affected++;
        }

        return affected;
    }
}
