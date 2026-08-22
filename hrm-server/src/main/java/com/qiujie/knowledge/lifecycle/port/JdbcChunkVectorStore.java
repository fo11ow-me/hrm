package com.qiujie.knowledge.lifecycle.port;

import com.pgvector.PGvector;
import com.qiujie.knowledge.entity.DocumentChunk;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.lifecycle.VectorMetadata;
import com.qiujie.knowledge.mapper.DocumentChunkMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * ChunkVectorStore 生产适配器：JdbcTemplate 直连 kb 数据源（PG，autocommit 不可回滚）。
 * 复用现有 DocumentChunkMapper 的切片 SQL；vector_store 与 kb_document 镜像表的 SQL 在此集中，
 * metadata JSON 一律经 {@link VectorMetadata} 生成（形状单一事实来源）。
 */
@Component
public class JdbcChunkVectorStore implements ChunkVectorStore {

    private final DocumentChunkMapper chunkMapper;
    private final JdbcTemplate kbJdbc;

    public JdbcChunkVectorStore(DocumentChunkMapper chunkMapper,
                                @Qualifier("kbDataSource") DataSource kbDataSource) {
        this.chunkMapper = chunkMapper;
        this.kbJdbc = new JdbcTemplate(kbDataSource);
    }

    @Override
    public List<ChunkRecord> storeChunks(Long documentId, List<ChunkDraft> drafts) {
        List<ChunkRecord> records = new ArrayList<>();
        for (ChunkDraft draft : drafts) {
            DocumentChunk entity = new DocumentChunk()
                    .setDocumentId(documentId)
                    .setChunkIndex(draft.chunkIndex())
                    .setChunkText(draft.chunkText())
                    .setTokenCount(draft.tokenCount())
                    .setCharStart(draft.charStart())
                    .setCharEnd(draft.charEnd())
                    .setMetadataJson("{\"chunk_index\":" + draft.chunkIndex() + "}");
            chunkMapper.insert(entity);
            records.add(new ChunkRecord(entity.getId(), draft));
        }
        return records;
    }

    @Override
    public void storeVectors(List<VectorDraft> vectors) {
        for (VectorDraft v : vectors) {
            PGvector pgVector = new PGvector(v.embedding());
            kbJdbc.update(
                    "INSERT INTO vector_store (id, content, metadata, embedding)"
                    + " VALUES (gen_random_uuid(), ?, ?::jsonb, ?)",
                    v.chunkText(),
                    VectorMetadata.toJson(v.documentId(), v.chunkId(), v.documentName()),
                    pgVector);
        }
    }

    @Override
    public void deleteChunks(Long documentId) {
        // 先向量后切片（与 pgvector 外键/检索一致性约定一致）
        kbJdbc.update("DELETE FROM vector_store WHERE metadata::jsonb ->> 'documentId' = ?",
                String.valueOf(documentId));
        kbJdbc.update("DELETE FROM document_chunk WHERE document_id = ?", documentId);
    }

    @Override
    public List<DocumentChunk> listChunks(Long documentId) {
        return chunkMapper.selectByDocumentIdOrderByChunkIndex(documentId);
    }

    @Override
    public void upsertDocumentMirror(KnowledgeDocument doc) {
        kbJdbc.update("""
                INSERT INTO kb_document (id, name, old_name, type, file_hash, file_size,
                                         status, staff_id, is_deleted, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, NOW(), NOW())
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, old_name = EXCLUDED.old_name,
                  type = EXCLUDED.type, file_hash = EXCLUDED.file_hash, file_size = EXCLUDED.file_size,
                  status = EXCLUDED.status, staff_id = EXCLUDED.staff_id, is_deleted = 0,
                  update_time = NOW()
                """, doc.getId(), doc.getName(), doc.getOldName(), doc.getType(),
                doc.getFileHash(), doc.getFileSize(), doc.getStatus(), doc.getStaffId());
    }

    @Override
    public void deleteDocumentMirror(Long documentId) {
        kbJdbc.update("DELETE FROM kb_document WHERE id = ?", documentId);
    }
}
