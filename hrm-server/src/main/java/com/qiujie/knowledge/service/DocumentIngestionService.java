package com.qiujie.knowledge.service;

import com.qiujie.knowledge.entity.DocumentChunk;
import com.qiujie.knowledge.entity.IngestionJob;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.enums.DocumentStatusEnum;
import com.qiujie.knowledge.mapper.DocumentChunkMapper;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.pgvector.PGvector;
import com.qiujie.storage.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档 ETL 管道编排。
 * <p>
 * 完整流程：清理旧产物 → 读取文件 → 解析 → 清洗 → 切片 → 持久化 → 向量化 → 标记 READY。
 * <p>
 * 调用入口：{@code DocumentIngestionListener} 在事务提交后异步触发，或
 * {@code KnowledgeService#retry} 手动重试时同步触发。
 *
 * @author quuj
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    private IngestionJobMapper jobMapper;

    @Autowired
    private DocumentParserService parserService;

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private TextCleanupService textCleanupService;

    @Autowired
    private DashScopeEmbeddingClient embeddingClient;

    private final JdbcTemplate kbJdbc;

    public DocumentIngestionService(@Qualifier("kbDataSource") DataSource kbDataSource) {
        this.kbJdbc = new JdbcTemplate(kbDataSource);
    }

    /**
     * 对指定文档执行 ETL 摄入管道。
     * <p>
     * 设计说明：
     * <ul>
     *   <li><b>重试</b>：通过 {@code @Retryable} 自动重试 3 次，退避策略 2s → 4s → 8s。
     *       耗尽后由 {@link #recover} 将文档标记为 FAILED。</li>
     *   <li><b>事务</b>：MySQL 侧（kb_document、ingestion_jobs）受 Spring 事务管理；
     *       PostgreSQL 侧（document_chunk、vector_store）通过 JdbcTemplate 直接提交，
     *       不在同一事务中。因此重试时需要先清理旧产物防止重复。</li>
     *   <li><b>状态机</b>：UPLOADED → PROCESSING → READY（成功）/ FAILED（失败）。</li>
 *   <li><b>可重入</b>：第 1 步清理旧切片和向量，保证重试时不会产生重复数据。</li>
     * </ul>
     *
     * @param documentId 文档 ID
     * @throws RuntimeException 摄入失败时抛出，触发 @Retryable 重试
     */
    @Retryable(retryFor = RuntimeException.class,
               maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2.0))
    @Transactional
    public void ingest(Long documentId) {
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new RuntimeException("文档不存在: " + documentId);
        }
        log.info("ETL processing: documentId={}, name={}", documentId, doc.getOldName());

        // 先推进状态为 PROCESSING，防止并发重复摄入
        doc.setStatus(DocumentStatusEnum.PROCESSING.name());
        documentMapper.updateById(doc);

        // 创建摄入任务记录，用于追踪每次 ETL 执行的耗时和结果
        IngestionJob job = new IngestionJob()
                .setDocumentId(documentId)
                .setStaffId(doc.getStaffId())
                .setJobType("INGEST_DOCUMENT")
                .setStatus("RUNNING")
                .setRetryCount(0)
                .setMaxRetries(3)
                .setStartedAt(LocalDateTime.now())
                .setCreateTime(LocalDateTime.now());
        jobMapper.insert(job);

        try {
            // 1. 清理旧产物：先删 vector_store 后删 document_chunk，保证重入时无重复数据
            //    PostgreSQL 侧直接提交，不做回滚
            chunkMapper.deleteByDocumentId(documentId);

            // 2. 从 MinIO 拉取原始文件流
            InputStream inputStream = storageService.get(doc.getName());

            // 3. 根据文件类型选择解析器（PDF → Apache PDFBox, DOCX → POI, MD/TXT → 原文）
            String rawText = parserService.parse(inputStream, doc.getType());

            // 4. 文本清洗：去除多余空格、控制字符，统一换行符
            String fullText = textCleanupService.clean(rawText);

            // 5. 按段落边界切片，每块不超过 max-size 字符，相邻块带 overlap 重叠
            //    切片在内存中执行，尚未持久化
            List<ChunkService.ChunkResult> chunks = chunkService.split(fullText);
            doc.setChunkCount(chunks.size());

            // 6. 持久化切片到 PostgreSQL document_chunk 表，逐条 INSERT 以获取自增 ID
            //    同时收集文本和 ID，供下一步向量化使用
            List<String> chunkTexts = new ArrayList<>();
            List<Long> chunkIds = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                ChunkService.ChunkResult cr = chunks.get(i);
                DocumentChunk entity = new DocumentChunk()
                        .setDocumentId(documentId)
                        .setChunkIndex(i)
                        .setChunkText(cr.getText())
                        .setTokenCount(cr.getTokenCount())
                        .setCharStart(cr.getCharStart())
                        .setCharEnd(cr.getCharEnd())
                        .setChunkSummary(null)  // LLM 摘要后续异步生成
                        .setMetadataJson("{\"chunk_index\":" + i + "}");
                chunkMapper.insert(entity);
                chunkTexts.add(cr.getText());
                chunkIds.add(entity.getId());
            }

            // 7. 调用 DashScope text-embedding-v4 批量向量化，写入 PostgreSQL vector_store
            //    embedding 通过 metadata JSON 中的 documentId / chunkId 关联回切片
            List<float[]> vectors = embeddingClient.embed(chunkTexts);
            for (int i = 0; i < chunkIds.size(); i++) {
                PGvector pgVector = new PGvector(vectors.get(i));
                kbJdbc.update(
                    "INSERT INTO vector_store (id, content, metadata, embedding)"
                    + " VALUES (gen_random_uuid(), ?, ?::jsonb, ?)",
                    chunkTexts.get(i),
                    "{\"documentId\":" + documentId + ",\"chunkId\":" + chunkIds.get(i) + "}",
                    pgVector);
            }
            log.info("Vectorized {} chunks for document={}", chunks.size(), documentId);

            // 8. 全部完成：更新文档状态为 READY，保存预览文本（前 500 字符）和完成时间
            doc.setStatus(DocumentStatusEnum.READY.name());
            doc.setProcessTime(LocalDateTime.now());
            doc.setPreviewText(fullText != null && fullText.length() > 500
                    ? fullText.substring(0, 500) : fullText);
            documentMapper.updateById(doc);

            // 标记摄入任务为成功
            job.setStatus("SUCCEEDED");
            job.setFinishedAt(LocalDateTime.now());
            jobMapper.updateById(job);

            log.info("ETL completed: documentId={}, chunks={}", documentId, chunks.size());
        } catch (Exception e) {
            // 摄入失败：记录错误原因到任务表，然后抛出异常触发 @Retryable
            // 注意此时 PROCESSING 状态保持不变，等待下次重试
            log.error("ETL failed: documentId={}", documentId, e);
            job.setStatus("FAILED");
            job.setLastError(truncate(e.getMessage(), 1000));
            job.setFinishedAt(LocalDateTime.now());
            jobMapper.updateById(job);
            throw e;
        }
    }

    /**
     * 重试耗尽后的兜底处理：将文档标记为 FAILED，记录失败原因。
     * <p>
     * 用户可在前端点击"重试"按钮，通过 {@code KnowledgeService#retry} 重新提交处理。
     */
    @Recover
    @Transactional
    public void recover(RuntimeException e, Long documentId) {
        log.error("ETL exhausted retries: documentId={}", documentId, e);
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc != null) {
            doc.setStatus(DocumentStatusEnum.FAILED.name());
            doc.setFailureReason(truncate(e.getMessage(), 500));
            documentMapper.updateById(doc);
        }
    }

    /** 截断字符串，防止错误信息超出数据库字段长度 */
    private static String truncate(String s, int maxLen) {
        if (s == null) return "未知错误";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
