package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.IngestionJob;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.enums.DocumentStatusEnum;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore.ChunkDraft;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore.ChunkRecord;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore.VectorDraft;
import com.qiujie.knowledge.lifecycle.port.EmbeddingProvider;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.qiujie.knowledge.service.ChunkService;
import com.qiujie.knowledge.service.DocumentParserService;
import com.qiujie.knowledge.service.TextCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ETL 摄入管道（模块内部组件，由 {@link DocumentLifecycleService} 在事务提交后触发）。
 * <p>
 * 完整流程：CAS 认领 → 建作业 → 清理旧产物 → 取文件 → 解析 → 清洗 → 切片 →
 * 持久化切片 → 向量化 → 写向量 → 同步镜像 → 结算 READY；失败 → FAILED + 原因。
 */
public final class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final KnowledgeDocumentMapper documentMapper;
    private final IngestionJobMapper jobMapper;
    private final ChunkVectorStore chunkVectorStore;
    private final ObjectStore objectStore;
    private final EmbeddingProvider embeddingProvider;
    private final DocumentParserService parserService;
    private final TextCleanupService textCleanupService;
    private final ChunkService chunkService;

    public IngestionPipeline(KnowledgeDocumentMapper documentMapper,
                             IngestionJobMapper jobMapper,
                             ChunkVectorStore chunkVectorStore,
                             ObjectStore objectStore,
                             EmbeddingProvider embeddingProvider,
                             DocumentParserService parserService,
                             TextCleanupService textCleanupService,
                             ChunkService chunkService) {
        this.documentMapper = documentMapper;
        this.jobMapper = jobMapper;
        this.chunkVectorStore = chunkVectorStore;
        this.objectStore = objectStore;
        this.embeddingProvider = embeddingProvider;
        this.parserService = parserService;
        this.textCleanupService = textCleanupService;
        this.chunkService = chunkService;
    }

    /** 执行摄入（工作线程调用，无 Spring 事务；PG 侧 best-effort）。 */
    void run(Long documentId) {
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            return;
        }
        // CAS 认领：并发触发/恢复重发只有一个管道能跑
        if (documentMapper.claimForProcessing(documentId) == 0) {
            return;
        }

        IngestionJob job = new IngestionJob()
                .setDocumentId(documentId)
                .setStaffId(doc.getStaffId())
                .setJobType("INGEST_DOCUMENT")
                .setStatus("RUNNING")
                .setStartedAt(LocalDateTime.now())
                .setCreateTime(LocalDateTime.now());
        jobMapper.insert(job);

        try {
            // 清理优先：PG 侧自动提交不可回滚，先删旧产物保证重入无重复
            chunkVectorStore.deleteChunks(documentId);

            String fullText;
            try (InputStream input = objectStore.getObject(doc.getName())) {
                fullText = textCleanupService.clean(parserService.parse(input, doc.getType()));
            }

            List<ChunkService.ChunkResult> chunks = chunkService.split(fullText);
            List<ChunkDraft> drafts = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                ChunkService.ChunkResult cr = chunks.get(i);
                drafts.add(new ChunkDraft(i, cr.getText(), cr.getTokenCount(), cr.getCharStart(), cr.getCharEnd()));
            }
            List<ChunkRecord> records = chunkVectorStore.storeChunks(documentId, drafts);

            // 向量化（失败条目 null 容忍），metadata 经 VectorMetadata 单点生成
            List<float[]> vectors = embeddingProvider.embedTexts(
                    records.stream().map(r -> r.draft().chunkText()).toList());
            List<VectorDraft> vectorDrafts = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                float[] vec = vectors.get(i);
                if (vec == null) {
                    continue;
                }
                ChunkRecord record = records.get(i);
                vectorDrafts.add(new VectorDraft(documentId, record.id(),
                        record.draft().chunkText(), doc.getOldName(), vec));
            }
            chunkVectorStore.storeVectors(vectorDrafts);

            // 同步 PG kb_document 镜像行（关键词检索/邻窗扩展的 JOIN 来源）
            chunkVectorStore.upsertDocumentMirror(doc.setStatus(DocumentStatusEnum.READY.name()));

            String preview = fullText.length() > 500 ? fullText.substring(0, 500) : fullText;
            if (documentMapper.completeProcessing(documentId, preview, chunks.size()) == 0) {
                // 执行中被删：不复活已删文档，清掉本次写入的残留产物
                chunkVectorStore.deleteChunks(documentId);
                chunkVectorStore.deleteDocumentMirror(documentId);
                job.setStatus("CANCELLED");
            } else {
                job.setStatus("SUCCEEDED");
            }
            job.setFinishedAt(LocalDateTime.now());
            jobMapper.updateById(job);

            log.info("ETL completed: documentId={}, chunks={}, vectors={}",
                    documentId, chunks.size(), vectorDrafts.size());
        } catch (Exception e) {
            log.error("ETL failed: documentId={}", documentId, e);
            job.setStatus("FAILED")
                    .setLastError(truncate(e.getMessage(), 1000))
                    .setFinishedAt(LocalDateTime.now());
            jobMapper.updateById(job);
            // 状态机补偿：文档立即置 FAILED + 原因（不再卡 PROCESSING 等重启）
            documentMapper.markFailed(documentId, truncate(e.getMessage(), 1000));
        }
    }

    /** 截断字符串，防止错误信息超出数据库字段长度 */
    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return "未知错误";
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
