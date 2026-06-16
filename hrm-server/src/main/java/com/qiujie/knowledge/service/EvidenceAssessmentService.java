package com.qiujie.knowledge.service;

import com.qiujie.knowledge.enums.EvidenceLevel;
import com.qiujie.knowledge.spi.KnowledgeSearchProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import com.qiujie.knowledge.spi.KnowledgeSearchProvider.SearchResult;

/**
 * 证据充分度评估服务。
 * 基于检索结果的数量、分数分布、文档多样性，判定证据等级。
 */
@Service
public class EvidenceAssessmentService {

    @Value("${knowledge.qa.evidence-none-threshold:0.0}")
    private double noneThreshold;

    @Value("${knowledge.qa.evidence-weak-threshold:0.3}")
    private double weakThreshold;

    @Value("${knowledge.qa.evidence-partial-threshold:0.5}")
    private double partialThreshold;

    @Value("${knowledge.qa.evidence-sufficient-threshold:0.7}")
    private double sufficientThreshold;

    public record Assessment(EvidenceLevel level, String reason) {}

    /**
     * 根据检索结果评估证据充分度。
     */
    public Assessment assess(List<SearchResult> results) {
        if (results.isEmpty()) {
            return new Assessment(EvidenceLevel.NONE, "知识库中未找到相关信息");
        }

        double maxScore = results.get(0).score();
        double avgScore = results.stream().mapToDouble(r -> r.score()).average().orElse(0);
        long uniqueDocs = results.stream().map(r -> r.documentId()).distinct().count();
        int count = results.size();

        // 多因素综合判定
        double compositeScore = maxScore * 0.4 + avgScore * 0.3
                + Math.min(uniqueDocs / 3.0, 1.0) * 0.15
                + Math.min(count / 5.0, 1.0) * 0.15;

        if (compositeScore >= sufficientThreshold) {
            return new Assessment(EvidenceLevel.SUFFICIENT,
                    String.format("找到 %d 条结果（%d 个文档），充分度 %.0f%%", count, uniqueDocs, compositeScore * 100));
        }
        if (compositeScore >= partialThreshold) {
            return new Assessment(EvidenceLevel.PARTIAL,
                    String.format("找到 %d 条部分相关结果，可能不够完整", count));
        }
        if (compositeScore >= weakThreshold) {
            return new Assessment(EvidenceLevel.WEAK,
                    String.format("仅找到 %d 条弱相关结果，信息可能不准确", count));
        }
        // NONE 只在真的没有任何结果时才触发，这里理论上不会到（results 非空）
        return new Assessment(EvidenceLevel.WEAK, "检索结果相关性较弱");
    }
}
