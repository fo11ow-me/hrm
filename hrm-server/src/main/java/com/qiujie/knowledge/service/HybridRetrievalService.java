package com.qiujie.knowledge.service;

import com.qiujie.knowledge.spi.KnowledgeSearchProvider;
import com.qiujie.knowledge.spi.KnowledgeSearchProvider.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务：向量检索 + 关键词检索 → RRF 融合。
 */
@Service
public class HybridRetrievalService implements KnowledgeSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalService.class);

    @Autowired
    private OllamaEmbeddingClient ollamaEmbedding;

    private final JdbcTemplate kbJdbc;

    public HybridRetrievalService(@Qualifier("kbDataSource") DataSource kbDataSource) {
        this.kbJdbc = new JdbcTemplate(kbDataSource);
    }

    @Value("${knowledge.retrieval.top-k:10}")
    private int topK;

    @Value("${knowledge.retrieval.vector-top-k:20}")
    private int vectorTopK;

    @Value("${knowledge.retrieval.keyword-top-k:20}")
    private int keywordTopK;

    @Value("${knowledge.retrieval.rrf-k:60}")
    private int rrfK;

    @Value("${knowledge.retrieval.window-size:1}")
    private int windowSize;

    public List<SearchResult> search(List<String> queries) {
        List<SearchResult> allResults = new ArrayList<>();
        for (String query : queries) {
            allResults.addAll(vectorSearch(query));
            allResults.addAll(keywordSearch(query));
        }
        if (allResults.isEmpty()) return List.of();
        List<SearchResult> fused = rrfFuse(allResults);
        fused.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (fused.size() > topK) fused = fused.subList(0, topK);
        return expandNeighborWindows(fused);
    }

    private List<SearchResult> vectorSearch(String query) {
        try {
            float[] vec = ollamaEmbedding.embed(List.of(query)).get(0);
            com.pgvector.PGvector pgVec = new com.pgvector.PGvector(vec);
            String sql = "SELECT content, metadata, 1 - (embedding <=> ?) AS similarity FROM vector_store ORDER BY embedding <=> ? LIMIT ?";
            return kbJdbc.query(sql, rs -> {
                List<SearchResult> list = new ArrayList<>();
                while (rs.next()) {
                    String metaStr = rs.getString("metadata");
                    double sim = rs.getDouble("similarity");
                    String docName = "";
                    long docId = 0, chunkId = 0;
                    if (metaStr != null) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode m = new com.fasterxml.jackson.databind.ObjectMapper().readTree(metaStr);
                            docName = m.path("documentName").asText("");
                            docId = m.path("documentId").asLong(0);
                            chunkId = m.path("chunkId").asLong(0);
                        } catch (Exception ignored) {}
                    }
                    list.add(new SearchResult(rs.getString("content"), docName, docId, chunkId, sim, "vector"));
                }
                return list;
            }, pgVec, pgVec, vectorTopK);
        } catch (Exception e) {
            log.warn("Vector search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<SearchResult> keywordSearch(String query) {
        if (kbJdbc == null) return List.of();
        try {
            String sql = """
                    SELECT c.chunk_text, d.old_name, d.id, c.id
                    FROM document_chunk c
                    JOIN kb_document d ON d.id = c.document_id
                    WHERE c.chunk_text ILIKE ? AND d.is_deleted = 0 AND d.status = 'READY'
                    ORDER BY length(c.chunk_text) ASC
                    LIMIT ?
                    """;
            String pattern = "%" + query.replace("%", "\\%").replace("_", "\\_") + "%";
            return kbJdbc.query(sql, (rs, rowNum) -> {
                double score = 0.6;
                String text = rs.getString(1);
                int count = countMatches(text.toLowerCase(), query.toLowerCase());
                score += Math.min(count * 0.1, 0.3);
                return new SearchResult(text, rs.getString(2), rs.getLong(3), rs.getLong(4), score, "keyword");
            }, pattern, keywordTopK);
        } catch (Exception e) {
            log.warn("Keyword search failed: {}", e.getMessage());
            return List.of();
        }
    }

    List<SearchResult> rrfFuse(List<SearchResult> results) {
        Map<String, List<SearchResult>> bySource = results.stream()
                .collect(Collectors.groupingBy(r -> r.source()));
        List<SearchResult> vectorRanked = bySource.getOrDefault("vector", List.of())
                .stream().sorted((a, b) -> Double.compare(b.score(), a.score())).toList();
        List<SearchResult> keywordRanked = bySource.getOrDefault("keyword", List.of())
                .stream().sorted((a, b) -> Double.compare(b.score(), a.score())).toList();
        Map<String, SearchResult> fused = new LinkedHashMap<>();
        int k = rrfK;
        for (int i = 0; i < vectorRanked.size(); i++) {
            SearchResult r = vectorRanked.get(i);
            String key = r.documentId() + ":" + r.chunkId();
            fused.put(key, new SearchResult(r.chunkText(), r.documentName(), r.documentId(), r.chunkId(), 1.0 / (k + i + 1), "rrf"));
        }
        for (int i = 0; i < keywordRanked.size(); i++) {
            SearchResult r = keywordRanked.get(i);
            String key = r.documentId() + ":" + r.chunkId();
            double rrf = 1.0 / (k + i + 1);
            if (fused.containsKey(key)) {
                SearchResult existing = fused.get(key);
                fused.put(key, new SearchResult(existing.chunkText(), existing.documentName(), existing.documentId(), existing.chunkId(), existing.score() + rrf, "rrf"));
            } else {
                fused.put(key, new SearchResult(r.chunkText(), r.documentName(), r.documentId(), r.chunkId(), rrf, "rrf"));
            }
        }
        double maxScore = fused.values().stream().mapToDouble(r -> r.score()).max().orElse(1.0);
        return fused.values().stream()
                .map(r -> new SearchResult(r.chunkText(), r.documentName(), r.documentId(), r.chunkId(), r.score() / maxScore, "rrf"))
                .sorted((a, b) -> Double.compare(b.score(), a.score())).collect(Collectors.toList());
    }

    private List<SearchResult> expandNeighborWindows(List<SearchResult> top) {
        if (kbJdbc == null || windowSize <= 0) return top;
        Set<String> existingKeys = top.stream().map(r -> r.documentId() + ":" + r.chunkId()).collect(Collectors.toSet());
        List<SearchResult> expanded = new ArrayList<>(top);
        for (SearchResult r : top) {
            if (r.chunkId() == null || r.documentId() == null) continue;
            try {
                String sql = """
                        SELECT chunk_text, old_name, d.id, c.id, c.chunk_index
                        FROM document_chunk c JOIN kb_document d ON d.id = c.document_id
                        WHERE d.id = ? AND c.chunk_index BETWEEN (SELECT chunk_index - ? FROM document_chunk WHERE id = ?)
                            AND (SELECT chunk_index + ? FROM document_chunk WHERE id = ?) AND d.status = 'READY'
                        """;
                kbJdbc.query(sql, (rs, rowNum) -> {
                    String key = rs.getLong(3) + ":" + rs.getLong(4);
                    if (!existingKeys.contains(key)) {
                        existingKeys.add(key);
                        expanded.add(new SearchResult(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), r.score() * 0.8, "neighbor"));
                    }
                    return null;
                }, r.documentId(), windowSize, r.chunkId(), windowSize, r.chunkId());
            } catch (Exception ignored) {}
        }
        return expanded;
    }

    private static double scoreToRelevance(double distanceOrSimilarity, boolean isDistance) {
        return isDistance ? Math.max(0, Math.min(1, 1.0 - distanceOrSimilarity)) : Math.max(0, Math.min(1, distanceOrSimilarity));
    }

    private static Long toLong(Object v) {
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof String s) try { return Long.parseLong(s); } catch (Exception ignored) {}
        return null;
    }

    private static int countMatches(String text, String query) {
        if (text == null || query == null) return 0;
        int count = 0, idx = 0;
        String lowerText = text.toLowerCase(), lowerQuery = query.toLowerCase();
        while ((idx = lowerText.indexOf(lowerQuery, idx)) != -1) { count++; idx += lowerQuery.length(); }
        return count;
    }
}
