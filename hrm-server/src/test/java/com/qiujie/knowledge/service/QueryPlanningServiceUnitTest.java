package com.qiujie.knowledge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("查询规划服务")
class QueryPlanningServiceUnitTest {

    private QueryPlanningService service;

    @BeforeEach
    void setUp() {
        service = new QueryPlanningService();
        // llmClient is null (not set) → fallback to DIRECT
    }

    @Test
    @DisplayName("无 LLM 时应降级到 DIRECT 策略")
    void plan_WithoutLlm_ShouldFallbackToDirect() {
        var plan = service.plan("年假有多少天", null);

        assertEquals("DIRECT", plan.strategy());
        assertEquals(1, plan.queries().size());
        assertEquals("年假有多少天", plan.queries().get(0));
    }

    @Test
    @DisplayName("短问题（<20字符）应直接 DIRECT")
    void plan_ShortQuestion_ShouldBeDirect() {
        var plan = service.plan("年假", null);

        assertEquals("DIRECT", plan.strategy());
        assertEquals(List.of("年假"), plan.queries());
    }

    @Test
    @DisplayName("指定 DIRECT 策略应直接使用")
    void plan_HintDirect_ShouldUseDirect() {
        var plan = service.plan("请假流程怎么走需要什么材料", "DIRECT");

        assertEquals("DIRECT", plan.strategy());
    }

    @Test
    @DisplayName("指定 REWRITE 策略（无 LLM）应降级到 DIRECT")
    void plan_HintRewrite_WithoutLlm_ShouldFallback() {
        var plan = service.plan("请假流程怎么走", "REWRITE");

        // Without LLM, rewrite returns original
        assertEquals("DIRECT", plan.strategy());
    }

    @Test
    @DisplayName("指定 DECOMPOSE 策略（无 LLM）应降级到 DIRECT")
    void plan_HintDecompose_WithoutLlm_ShouldFallback() {
        var plan = service.plan("请假和加班的流程分别是什么", "DECOMPOSE");

        // Without LLM, decompose returns original
        assertEquals("DIRECT", plan.strategy());
    }

    @Test
    @DisplayName("AUTO 策略（无 LLM）应降级到 DIRECT")
    void plan_Auto_WithoutLlm_ShouldFallbackToDirect() {
        var plan = service.plan("公司有多少天年假，如何申请", "AUTO");

        assertEquals("DIRECT", plan.strategy());
    }

    @Test
    @DisplayName("questions 列表不应为空")
    void plan_Queries_ShouldNeverBeEmpty() {
        var plan = service.plan("", null);

        assertNotNull(plan.queries());
        assertFalse(plan.queries().isEmpty());
    }
}
