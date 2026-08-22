package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文档物理清理（模块内部组件）：引用计数 → MinIO 物理文件 → PG 切片/向量/镜像。
 * <p>
 * 整体幂等可重跑——崩溃于中途的残留由启动恢复重调 purge 兜底。
 */
final class DocumentPurgeHandler {

    private static final Logger log = LoggerFactory.getLogger(DocumentPurgeHandler.class);

    private final KnowledgeDocumentMapper documentMapper;
    private final ChunkVectorStore chunkVectorStore;
    private final ObjectStore objectStore;

    DocumentPurgeHandler(KnowledgeDocumentMapper documentMapper,
                         ChunkVectorStore chunkVectorStore,
                         ObjectStore objectStore) {
        this.documentMapper = documentMapper;
        this.chunkVectorStore = chunkVectorStore;
        this.objectStore = objectStore;
    }

    /** 清理文档的物理产物；幂等。 */
    void purge(Long documentId, String storageKey) {
        // 引用计数只统计存活文档：为 0 才删物理文件（同一文件可能被其他文档共享）
        if (documentMapper.countLiveByFileName(storageKey, documentId) == 0) {
            try {
                objectStore.deleteObject(storageKey);
            } catch (Exception e) {
                log.warn("删除物理文件失败: {}", storageKey, e);
            }
        }
        chunkVectorStore.deleteChunks(documentId);
        chunkVectorStore.deleteDocumentMirror(documentId);
    }
}
