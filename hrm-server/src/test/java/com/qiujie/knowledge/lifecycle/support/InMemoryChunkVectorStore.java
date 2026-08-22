package com.qiujie.knowledge.lifecycle.support;

import com.qiujie.knowledge.entity.DocumentChunk;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ChunkVectorStore 内存假适配器：状态机全链路测试零数据库。
 * 复刻生产语义：deleteChunks 先向量后切片；storeChunks 回填自增 id。
 */
public class InMemoryChunkVectorStore implements ChunkVectorStore {

    private final Map<Long, List<DocumentChunk>> chunksByDoc = new ConcurrentHashMap<>();
    private final List<VectorDraft> vectors = new CopyOnWriteArrayList<>();
    private final Map<Long, KnowledgeDocument> mirror = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override
    public List<ChunkRecord> storeChunks(Long documentId, List<ChunkDraft> drafts) {
        List<DocumentChunk> list = chunksByDoc.computeIfAbsent(documentId, k -> new ArrayList<>());
        List<ChunkRecord> records = new ArrayList<>();
        for (ChunkDraft draft : drafts) {
            long id = idSeq.getAndIncrement();
            list.add(new DocumentChunk()
                    .setId(id)
                    .setDocumentId(documentId)
                    .setChunkIndex(draft.chunkIndex())
                    .setChunkText(draft.chunkText())
                    .setTokenCount(draft.tokenCount())
                    .setCharStart(draft.charStart())
                    .setCharEnd(draft.charEnd()));
            records.add(new ChunkRecord(id, draft));
        }
        return records;
    }

    @Override
    public void storeVectors(List<VectorDraft> vectorDrafts) {
        vectors.addAll(vectorDrafts);
    }

    @Override
    public void deleteChunks(Long documentId) {
        vectors.removeIf(v -> v.documentId() == documentId.longValue());
        chunksByDoc.remove(documentId);
    }

    @Override
    public List<DocumentChunk> listChunks(Long documentId) {
        return chunksByDoc.getOrDefault(documentId, List.of()).stream()
                .sorted(Comparator.comparing(DocumentChunk::getChunkIndex))
                .toList();
    }

    @Override
    public void upsertDocumentMirror(KnowledgeDocument doc) {
        mirror.put(doc.getId(), doc);
    }

    @Override
    public void deleteDocumentMirror(Long documentId) {
        mirror.remove(documentId);
    }

    // ==================== 断言辅助 ====================

    public List<VectorDraft> vectors() {
        return List.copyOf(vectors);
    }

    public KnowledgeDocument mirror(Long documentId) {
        return mirror.get(documentId);
    }

    public boolean mirrorRemoved(Long documentId) {
        return !mirror.containsKey(documentId);
    }
}
