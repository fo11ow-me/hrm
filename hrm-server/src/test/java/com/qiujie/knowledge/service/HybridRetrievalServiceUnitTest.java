package com.qiujie.knowledge.service;

import com.qiujie.knowledge.spi.KnowledgeSearchProvider.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("混合检索 — RRF 融合")
class HybridRetrievalServiceUnitTest {

    private HybridRetrievalService service;

    @BeforeEach
    void setUp() {
        // rrfFuse 不触碰 kbJdbc，mock DataSource 仅满足 JdbcTemplate 构造断言
        service = new HybridRetrievalService(mock(DataSource.class));
        ReflectionTestUtils.setField(service, "rrfK", 60);
        ReflectionTestUtils.setField(service, "windowSize", 0); // disable neighbor expansion
    }

    @Test
    @DisplayName("空输入应返回空列表")
    void rrfFuse_EmptyInput_ShouldReturnEmpty() {
        List<SearchResult> result = service.rrfFuse(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("仅向量通道结果应原样返回并归一化")
    void rrfFuse_OnlyVector_ShouldNormalize() {
        List<SearchResult> input = List.of(
                sr("文档A", 1L, 1L, 0.9, "vector"),
                sr("文档B", 2L, 2L, 0.7, "vector")
        );

        List<SearchResult> result = service.rrfFuse(input);

        assertEquals(2, result.size());
        // 最高分应归一化为 1.0
        assertEquals(1.0, result.get(0).score(), 0.001);
        assertTrue(result.get(0).score() >= result.get(1).score(),
                "Results should be sorted by score descending");
    }

    @Test
    @DisplayName("双通道相同切片应合并分数")
    void rrfFuse_SameChunkBothChannels_ShouldCombineScores() {
        List<SearchResult> input = List.of(
                sr("文档A", 1L, 1L, 0.9, "vector"),
                sr("文档A", 1L, 1L, 0.6, "keyword")
        );

        List<SearchResult> result = service.rrfFuse(input);

        assertEquals(1, result.size(), "Same chunk should be merged into one result");
    }

    @Test
    @DisplayName("双通道不同切片应全部保留")
    void rrfFuse_DifferentChunks_ShouldKeepAll() {
        List<SearchResult> input = List.of(
                sr("文档A", 1L, 1L, 0.9, "vector"),
                sr("文档B", 2L, 2L, 0.6, "keyword")
        );

        List<SearchResult> result = service.rrfFuse(input);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("RRF 分数应在 [0, 1] 范围内")
    void rrfFuse_Scores_ShouldBeInRange() {
        List<SearchResult> input = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            input.add(sr("文档" + i, (long) i, (long) i, 0.5 + i * 0.02, i % 2 == 0 ? "vector" : "keyword"));
        }

        List<SearchResult> result = service.rrfFuse(input);

        for (SearchResult r : result) {
            assertTrue(r.score() >= 0.0 && r.score() <= 1.0,
                    "Score " + r.score() + " should be in [0, 1]");
        }
    }

    @Test
    @DisplayName("结果应按分数降序排列")
    void rrfFuse_Sorting_ShouldBeDescending() {
        List<SearchResult> input = List.of(
                sr("A", 1L, 1L, 0.5, "vector"),
                sr("B", 2L, 2L, 0.9, "vector"),
                sr("C", 3L, 3L, 0.3, "keyword")
        );

        List<SearchResult> result = service.rrfFuse(input);

        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).score() >= result.get(i + 1).score(),
                    "Result[" + i + "]=" + result.get(i).score() + " should be >= [" + (i + 1) + "]=" + result.get(i + 1).score());
        }
    }

    @Test
    @DisplayName("大量结果的 RRF 融合应稳定")
    void rrfFuse_LargeInput_ShouldBeStable() {
        List<SearchResult> input = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            input.add(sr("文档" + (i / 10), (long) i, (long) i, Math.random(), i % 3 == 0 ? "vector" : "keyword"));
        }

        List<SearchResult> result = service.rrfFuse(input);
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= input.size());
    }

    private static SearchResult sr(String docName, Long docId, Long chunkId, double score, String source) {
        return new SearchResult("text-" + chunkId, docName, docId, chunkId, score, source);
    }
}
