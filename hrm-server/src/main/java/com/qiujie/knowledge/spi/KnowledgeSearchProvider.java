package com.qiujie.knowledge.spi;

import java.util.List;

/**
 * 知识库检索抽象。assistant/ 依赖此接口，由 knowledge/ 实现。
 */
public interface KnowledgeSearchProvider {

    record SearchResult(String chunkText, String documentName, Long documentId,
                         Long chunkId, double score, String source) {}

    List<SearchResult> search(List<String> queries);
}
