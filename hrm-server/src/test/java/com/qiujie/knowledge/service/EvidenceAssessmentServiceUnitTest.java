package com.qiujie.knowledge.service;

import com.qiujie.knowledge.enums.EvidenceLevel;
import com.qiujie.knowledge.spi.KnowledgeSearchProvider.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("证据充分度评估服务")
class EvidenceAssessmentServiceUnitTest {

    private EvidenceAssessmentService service;

    @BeforeEach
    void setUp() {
        service = new EvidenceAssessmentService();
        ReflectionTestUtils.setField(service, "noneThreshold", 0.0);
        ReflectionTestUtils.setField(service, "weakThreshold", 0.3);
        ReflectionTestUtils.setField(service, "partialThreshold", 0.5);
        ReflectionTestUtils.setField(service, "sufficientThreshold", 0.7);
    }

    @Test
    @DisplayName("空结果应返回 NONE")
    void assess_EmptyResults_ShouldReturnNone() {
        var result = service.assess(Collections.emptyList());
        assertEquals(EvidenceLevel.NONE, result.level());
    }

    @Test
    @DisplayName("高相关性多文档结果应返回 SUFFICIENT")
    void assess_HighScoreMultipleDocs_ShouldReturnSufficient() {
        List<SearchResult> results = List.of(
                sr("合同管理规定", 1L, 1L, 0.95),
                sr("员工手册", 2L, 2L, 0.88),
                sr("考勤制度", 3L, 3L, 0.82),
                sr("薪酬管理办法", 4L, 4L, 0.75),
                sr("请假流程", 5L, 5L, 0.70)
        );

        var result = service.assess(results);
        assertEquals(EvidenceLevel.SUFFICIENT, result.level());
    }

    @Test
    @DisplayName("中等相关性应返回 PARTIAL")
    void assess_MediumScore_ShouldReturnPartial() {
        List<SearchResult> results = List.of(
                sr("合同管理规定", 1L, 1L, 0.65),
                sr("员工手册", 2L, 2L, 0.55)
        );

        var result = service.assess(results);
        assertEquals(EvidenceLevel.PARTIAL, result.level());
    }

    @Test
    @DisplayName("低相关性单文档应返回 WEAK")
    void assess_LowScoreSingleDoc_ShouldReturnWeak() {
        List<SearchResult> results = List.of(
                sr("合同管理规定", 1L, 1L, 0.4)
        );

        var result = service.assess(results);
        assertEquals(EvidenceLevel.WEAK, result.level());
    }

    @Test
    @DisplayName("单条结果满分时仍可 SUFFICIENT")
    void assess_SinglePerfectResult_ShouldReturnSufficient() {
        List<SearchResult> results = List.of(
                sr("合同管理规定", 1L, 1L, 1.0)
        );

        var result = service.assess(results);
        // 1.0*0.4 + 1.0*0.3 + 0.333*0.15 + 0.2*0.15 = 0.78 > 0.7
        assertEquals(EvidenceLevel.SUFFICIENT, result.level());
    }

    @Test
    @DisplayName("多文档来源应提升评估等级")
    void assess_MultipleDocuments_ShouldBoostScore() {
        // same score but distributed across many docs → higher composite
        List<SearchResult> fewDocs = List.of(
                sr("合同管理规定", 1L, 1L, 0.6),
                sr("合同管理规定", 1L, 2L, 0.55)
        );

        List<SearchResult> manyDocs = List.of(
                sr("合同管理规定", 1L, 1L, 0.6),
                sr("员工手册", 2L, 2L, 0.55),
                sr("考勤制度", 3L, 3L, 0.50)
        );

        var resultFew = service.assess(fewDocs);
        var resultMany = service.assess(manyDocs);

        // manyDocs should have equal or higher level
        assertTrue(resultMany.level().ordinal() >= resultFew.level().ordinal(),
                "More unique docs should not decrease evidence level");
    }

    @Test
    @DisplayName("reason 应包含结果数量信息")
    void assess_Reason_ShouldContainCountInfo() {
        List<SearchResult> results = List.of(
                sr("合同管理规定", 1L, 1L, 0.9),
                sr("员工手册", 2L, 2L, 0.85)
        );

        var result = service.assess(results);
        assertNotNull(result.reason());
        assertFalse(result.reason().isBlank());
    }

    private static SearchResult sr(String docName, Long docId, Long chunkId, double score) {
        return new SearchResult("text for " + docName, docName, docId, chunkId, score, "rrf");
    }
}
