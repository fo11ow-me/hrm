package com.qiujie.knowledge.lifecycle.port;

import com.qiujie.knowledge.entity.DocumentChunk;
import com.qiujie.knowledge.entity.KnowledgeDocument;

import java.util.List;

/**
 * PG 切片与向量存储端口（document_chunk / vector_store / kb_document 镜像，kb 数据源）。
 * <p>
 * 全部写入幂等可清理——{@link #deleteChunks} 是重入与补偿的唯一清理入口（先向量后切片）。
 * 方法粒度为业务语义（storeChunks/deleteChunks），逐条 INSERT、自增 ID 回填等是适配器内部细节。
 */
public interface ChunkVectorStore {

    /** 切片写入草稿。 */
    record ChunkDraft(int chunkIndex, String chunkText, int tokenCount, int charStart, int charEnd) {}

    /** 切片写入结果（携带 PG 生成 id）。 */
    record ChunkRecord(long id, ChunkDraft draft) {}

    /**
     * 向量写入草稿。documentName 取 kb_document.old_name 快照——
     * 修复"写方只写 documentId/chunkId、读方读 documentName"的契约 bug。
     */
    record VectorDraft(long documentId, long chunkId, String chunkText, String documentName, float[] embedding) {}

    /** 批量写入切片并回填 PG 自增 id（向量写入需要 chunkId）。 */
    List<ChunkRecord> storeChunks(Long documentId, List<ChunkDraft> drafts);

    /** 批量写入向量；metadata JSON 一律经 VectorMetadata 生成（形状单一事实来源）。 */
    void storeVectors(List<VectorDraft> vectors);

    /** 幂等清理某文档全部检索产物（先向量后切片，不存在也成功）。 */
    void deleteChunks(Long documentId);

    /** 只读：按 chunk_index 升序返回切片。 */
    List<DocumentChunk> listChunks(Long documentId);

    /**
     * 同步 PG kb_document 镜像行（ETL 成功后调用），供关键词检索/邻窗扩展本地 JOIN。
     * 镜像此前从未被写入，导致关键词检索恒空。
     */
    void upsertDocumentMirror(KnowledgeDocument doc);

    /** 删除镜像行（删除文档时调用）。 */
    void deleteDocumentMirror(Long documentId);
}
