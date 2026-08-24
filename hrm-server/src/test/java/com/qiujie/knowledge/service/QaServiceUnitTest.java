package com.qiujie.knowledge.service;

import com.qiujie.knowledge.dto.QaRequest;
import com.qiujie.knowledge.enums.EvidenceLevel;
import com.qiujie.knowledge.service.EvidenceAssessmentService.Assessment;
import com.qiujie.knowledge.spi.KnowledgeSearchProvider.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link QaService} 编排行为测试。
 * <p>
 * 覆盖 streamAsk 的公共行为边界：空问题、NONE 证据、正常流程、LLM 失败降级。
 * 子服务全部 mock，验证委托关系与持久化。
 * </p>
 */
@DisplayName("QaService 编排行为")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QaServiceUnitTest {

    @Mock
    private QueryPlanningService planningService;
    @Mock
    private HybridRetrievalService retrievalService;
    @Mock
    private EvidenceAssessmentService evidenceService;
    @Mock
    private RAGPrompts ragPrompts;
    @Mock
    private QaRecordRepository qaRecordRepository;
    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.initialize();
    }

    /** 构建一个 QaService——chatClient 通过 Builder 注入（final 字段，须构造传入）。 */
    private QaService buildService() {
        QaService svc = new QaService(chatClientBuilder, executor);
        inject(svc, "planningService", planningService);
        inject(svc, "retrievalService", retrievalService);
        inject(svc, "evidenceService", evidenceService);
        inject(svc, "ragPrompts", ragPrompts);
        inject(svc, "qaRecordRepository", qaRecordRepository);
        return svc;
    }

    /** stub LLM 链：build() → prompt() → user() → call() → content()。 */
    private void stubLlm(String content) {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(content);
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private QaRequest request(String question) {
        QaRequest r = new QaRequest();
        r.setQuestion(question);
        return r;
    }

    /** 等待 SSE 完成（线程池任务执行完毕）。 */
    private void awaitCompletion(SseEmitter emitter, long timeoutMs) {
        CountDownLatch latch = new CountDownLatch(1);
        emitter.onCompletion(latch::countDown);
        emitter.onError(e -> latch.countDown());
        emitter.onTimeout(latch::countDown);
        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 测试 ====================

    @Test
    @DisplayName("空问题 → 立即返回 emitter，不提交线程池、不调子服务")
    void emptyQuestion_noSubServices() {
        QaService svc = buildService();
        SseEmitter emitter = svc.streamAsk(request(""));

        assertNotNull(emitter);
        verifyNoInteractions(planningService, retrievalService, evidenceService, qaRecordRepository);
    }

    @Test
    @DisplayName("NONE 证据 → 不调 ragPrompts（即不调 LLM），持久化 NONE 记录")
    void noneEvidence_skipsLlm() {
        when(planningService.plan(anyString(), any())).thenReturn(
                new QueryPlanningService.Plan("DIRECT", List.of("test")));
        when(retrievalService.search(anyList())).thenReturn(List.of());
        when(evidenceService.assess(anyList()))
                .thenReturn(new Assessment(EvidenceLevel.NONE, "未找到相关信息"));

        QaService svc = buildService();
        SseEmitter emitter = svc.streamAsk(request("年假多少天"));
        awaitCompletion(emitter, 3000);

        verify(qaRecordRepository).save(anyString(), anyString(), eq("NONE"), eq(0));
        verify(ragPrompts, never()).buildAnswerPrompt(anyString(), anyString(), any());
        verify(chatClient, never()).prompt();
    }

    @Test
    @DisplayName("正常流程 → 调 ragPrompts + LLM + 持久化 SUFFICIENT")
    void normalFlow_fullPipeline() {
        when(planningService.plan(anyString(), any())).thenReturn(
                new QueryPlanningService.Plan("DIRECT", List.of("年假")));
        var result = new SearchResult("年假5天", "考勤制度", 1L, 1L, 0.9, "vector");
        when(retrievalService.search(anyList())).thenReturn(List.of(result));
        when(evidenceService.assess(anyList()))
                .thenReturn(new Assessment(EvidenceLevel.SUFFICIENT, "找到 1 条结果"));
        when(ragPrompts.buildAnswerPrompt(anyString(), anyString(), any()))
                .thenReturn("mock prompt");
        stubLlm("根据考勤制度，年假为5天。");

        QaService svc = buildService();
        SseEmitter emitter = svc.streamAsk(request("年假多少天"));
        awaitCompletion(emitter, 3000);

        verify(ragPrompts).buildAnswerPrompt(anyString(), anyString(), any());
        verify(chatClient).prompt();
        verify(qaRecordRepository).save(anyString(), anyString(), eq("SUFFICIENT"), eq(1));
    }

    @Test
    @DisplayName("LLM 失败 → 降级 fallback，仍持久化记录")
    void llmFailure_fallsBackAndPersists() {
        when(planningService.plan(anyString(), any())).thenReturn(
                new QueryPlanningService.Plan("DIRECT", List.of("年假")));
        var result = new SearchResult("年假5天", "考勤制度", 1L, 1L, 0.9, "vector");
        when(retrievalService.search(anyList())).thenReturn(List.of(result));
        when(evidenceService.assess(anyList()))
                .thenReturn(new Assessment(EvidenceLevel.PARTIAL, "部分相关"));
        when(ragPrompts.buildAnswerPrompt(anyString(), anyString(), any()))
                .thenReturn("mock prompt");
        // LLM 抛异常
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("LLM timeout"));

        QaService svc = buildService();
        SseEmitter emitter = svc.streamAsk(request("年假多少天"));
        awaitCompletion(emitter, 3000);

        // LLM 失败仍持久化（fallback 回答）
        verify(qaRecordRepository).save(anyString(), anyString(), eq("PARTIAL"), eq(1));
        verify(chatClient).prompt();
    }

    @Test
    @DisplayName("编排顺序：先规划 → 再检索 → 再评估")
    void orchestrationOrder() {
        when(planningService.plan(anyString(), any())).thenReturn(
                new QueryPlanningService.Plan("DIRECT", List.of("年假")));
        when(retrievalService.search(anyList())).thenReturn(List.of());
        when(evidenceService.assess(anyList()))
                .thenReturn(new Assessment(EvidenceLevel.NONE, "无结果"));

        QaService svc = buildService();
        SseEmitter emitter = svc.streamAsk(request("年假多少天"));
        awaitCompletion(emitter, 3000);

        // 按顺序验证调用
        inOrder(planningService, retrievalService, evidenceService)
                .verify(planningService).plan(anyString(), any());
        inOrder(planningService, retrievalService, evidenceService)
                .verify(retrievalService).search(anyList());
        inOrder(planningService, retrievalService, evidenceService)
                .verify(evidenceService).assess(anyList());
    }
}