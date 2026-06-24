package com.qiujie.knowledge.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.qiujie.knowledge.spi.KnowledgeSearchProvider.SearchResult;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.dto.QaRequest;
import com.qiujie.knowledge.dto.QaResponse;
import com.qiujie.knowledge.enums.EvidenceLevel;
import com.qiujie.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;
import org.springframework.security.core.Authentication;
import java.util.stream.Collectors;

/**
 * 知识库 RAG 问答编排服务。
 * 查询规划 → 混合检索 → 证据评估 → LLM 生成 → 引用溯源
 */
@Service
public class QaService {

    private static final Logger log = LoggerFactory.getLogger(QaService.class);

    @Autowired
    private QueryPlanningService planningService;

    @Autowired
    private HybridRetrievalService retrievalService;

    @Autowired
    private EvidenceAssessmentService evidenceService;

    @Autowired
    private SecurityUtil securityUtil;

    private final ChatClient chatClient;

    public QaService(ChatClient.Builder chatClientBuilder,
                     @Autowired(required = false) @org.springframework.beans.factory.annotation.Qualifier("kbDataSource") DataSource kbDataSource) {
        this.chatClient = chatClientBuilder.build();
        this.kbJdbc = kbDataSource != null ? new JdbcTemplate(kbDataSource) : null;
    }

    private final JdbcTemplate kbJdbc;

    @Value("${knowledge.qa.max-context-chars:3000}")
    private int maxContextChars;

    /**
     * 问答入口。
     */
    public ResponseDTO ask(QaRequest request) {
        String question = request.getQuestion();
        if (question == null || question.isBlank()) {
            return com.qiujie.dto.Response.error("问题不能为空");
        }

        // 1. 查询规划
        var plan = planningService.plan(question, request.getStrategy());
        log.info("QA plan: strategy={}, queries={}", plan.strategy(), plan.queries());

        // 2. 混合检索
        List<SearchResult> results = retrievalService.search(plan.queries());

        // 3. 证据评估
        var assessment = evidenceService.assess(results);

        // 4. 构建上下文 + LLM 生成
        String answer;
        if (assessment.level() == EvidenceLevel.NONE) {
            answer = "抱歉，知识库中暂未找到与您问题相关的信息。建议查阅相关制度文件或联系管理员补充资料。";
        } else {
            answer = generateAnswer(question, results, assessment);
        }

        // 5. 构建引用
        List<QaResponse.CitationVO> citations = buildCitations(results);

        // 6. 构建响应
        QaResponse response = new QaResponse();
        response.setAnswer(answer);
        response.setEvidenceLevel(assessment.level().name());
        response.setStrategy(plan.strategy());
        response.setCitations(citations);
        response.setConversationId(IdUtil.fastSimpleUUID());

        // 7. 持久化 QA 记录
        saveQaRecord(question, answer, assessment.level().name(), citations);

        return com.qiujie.dto.Response.success(response);
    }

    /**
     * SSE 流式问答。捕获当前线程安全上下文传入后台线程。
     */
    public SseEmitter streamAsk(QaRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        new Thread(() -> {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                ResponseDTO result = ask(request);
                QaResponse qa = (QaResponse) result.getData();

                if (qa != null) {
                    // 流式发送 token
                    String answer = qa.getAnswer();
                    for (int i = 0; i < answer.length(); i += 3) {
                        String chunk = answer.substring(i, Math.min(i + 3, answer.length()));
                        emitter.send(SseEmitter.event().name("token").data(chunk));
                    }

                    // 发送引用和元数据
                    emitter.send(SseEmitter.event().name("citations").data(JSON.toJSONString(qa.getCitations())));
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("evidenceLevel", qa.getEvidenceLevel());
                    meta.put("strategy", qa.getStrategy());
                    meta.put("conversationId", qa.getConversationId());
                    emitter.send(SseEmitter.event().name("meta").data(meta));
                }
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("SSE stream error", e);
                emitter.completeWithError(e);
            } finally {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }).start();

        return emitter;
    }

    private String generateAnswer(String question, List<SearchResult> results,
                                   EvidenceAssessmentService.Assessment assessment) {
        // 构建参考资料上下文
        List<Map<String, String>> references = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        int charCount = 0;

        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            String snippet = String.format("[%d] 《%s》: %s\n",
                    i + 1, r.documentName(), r.chunkText());
            if (charCount + snippet.length() > maxContextChars) break;
            context.append(snippet);
            charCount += snippet.length();

            Map<String, String> ref = new HashMap<>();
            ref.put("index", String.valueOf(i + 1));
            ref.put("document", r.documentName());
            ref.put("text", r.chunkText());
            ref.put("score", String.format("%.2f", r.score()));
            references.add(ref);
        }

        String prompt = buildPrompt(question, context.toString(), assessment);
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("LLM generation failed: {}", e.getMessage());
            return fallbackAnswer(results, assessment);
        }
    }

    private String buildPrompt(String question, String context, EvidenceAssessmentService.Assessment assessment) {
        return """
                你是 HRM 系统的知识库助手，请根据以下参考资料回答用户问题。

                证据充分度：%s - %s

                参考资料：
                %s

                要求：
                - 仅基于参考资料回答，不要臆测
                - 如果资料不足以完全回答问题，明确说明局限性
                - 回答时引用参考资料的编号，例如「根据[1]...」
                - 回答简洁准确，用中文
                """.formatted(assessment.level().name(), assessment.reason(), context);
    }

    private String fallbackAnswer(List<SearchResult> results,
                                   EvidenceAssessmentService.Assessment assessment) {
        if (results.isEmpty()) return "未找到相关信息。";
        StringBuilder sb = new StringBuilder("检索到以下相关内容：\n\n");
        for (int i = 0; i < Math.min(results.size(), 3); i++) {
            var r = results.get(i);
            sb.append(String.format("[%d] 《%s》: %s\n\n", i + 1, r.documentName(), r.chunkText()));
        }
        if (assessment.level() == EvidenceLevel.WEAK) {
            sb.append("注意：检索结果相关性较弱，仅供参考。");
        }
        return sb.toString();
    }

    private List<QaResponse.CitationVO> buildCitations(List<SearchResult> results) {
        return results.stream()
                .limit(5)
                .map(r -> new QaResponse.CitationVO(
                        r.documentName(),
                        truncateText(r.chunkText(), 200),
                        r.score()))
                .collect(Collectors.toList());
    }

    private void saveQaRecord(String question, String answer, String evidenceLevel,
                               List<QaResponse.CitationVO> citations) {
        if (kbJdbc == null) return;
        try {
            kbJdbc.update(
                    "INSERT INTO kb_qa_record (question, answer, staff_id, evidence_level) VALUES (?, ?, ?, ?)",
                    question, answer, securityUtil.getCurrentOperatorId(), evidenceLevel);
        } catch (Exception e) {
            log.warn("Failed to save QA record", e);
        }
    }

    private String truncateText(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
