package com.qiujie.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.knowledge.entity.DocumentChunk;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.enums.DocumentStatusEnum;
import com.qiujie.knowledge.mapper.DocumentChunkMapper;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 文档 ETL 管道编排（7 阶段）。
 * 1.清理旧产物 → 2.读取 → 3.解析+清洗 → 4.切片 → 5.持久化 → 6.向量化 → 7.标记READY
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
    private DocumentParserService parserService;

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private TextCleanupService textCleanupService;

    @Autowired
    private OllamaEmbeddingClient ollamaEmbedding;

    private final JdbcTemplate kbJdbc;

    public DocumentIngestionService(@Qualifier("kbDataSource") DataSource kbDataSource) {
        this.kbJdbc = new JdbcTemplate(kbDataSource);
    }

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

        // 1. 清理旧产物
        chunkMapper.deleteByDocumentId(documentId);

        // 2. 读取文件
        InputStream inputStream = storageService.get(doc.getName());

        // 3. 解析文档
        String rawText = parserService.parse(inputStream, doc.getType());

        // 4. 文本清洗
        String fullText = textCleanupService.clean(rawText);

        // 5. 文本切片
        List<ChunkService.ChunkResult> chunks = chunkService.split(fullText);
        doc.setChunkCount(chunks.size());

        // 6. 持久化切片
        List<String> chunkTexts = new ArrayList<>();
        List<Long> chunkIds = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkService.ChunkResult cr = chunks.get(i);
            DocumentChunk entity = new DocumentChunk()
                    .setDocumentId(documentId)
                    .setChunkIndex(i)
                    .setChunkText(cr.getText())
                    .setTokenCount(cr.getTokenCount());
            chunkMapper.insert(entity);
            chunkTexts.add(cr.getText());
            chunkIds.add(entity.getId());
        }

        // 7. 向量化存储 — OllamaEmbeddingClient + PGvector JDBC
        List<float[]> vectors = ollamaEmbedding.embed(chunkTexts);
        for (int i = 0; i < chunkIds.size(); i++) {
            PGvector pgVector = new PGvector(vectors.get(i));
            kbJdbc.update("INSERT INTO vector_store (id, content, metadata, embedding) VALUES (gen_random_uuid(), ?, ?::jsonb, ?)",
                    chunkTexts.get(i), "{\"documentId\":" + documentId + ",\"chunkId\":" + chunkIds.get(i) + "}", pgVector);
        }
        log.info("Vectorized {} chunks for document={}", chunks.size(), documentId);

        // 8. 标记完成
        doc.setStatus(DocumentStatusEnum.READY.name());
        documentMapper.updateById(doc);
        log.info("ETL completed: documentId={}, chunks={}", documentId, chunks.size());
    }

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

    private static String truncate(String s, int maxLen) {
        if (s == null) return "未知错误";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
