package com.qiujie.knowledge.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qiujie.common.llm.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 查询规划服务。
 * 分析用户问题，决定检索策略：DIRECT / REWRITE / DECOMPOSE
 */
@Service
public class QueryPlanningService {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanningService.class);

    @Autowired(required = false)
    private LlmProvider llmClient;

    @Value("${knowledge.qa.strategy-limit:3}")
    private int strategyLimit;

    /** 规划结果 */
    public record Plan(String strategy, List<String> queries) {
        public Plan {
            queries = queries != null ? queries : List.of();
        }
    }

    private static final String PLANNING_PROMPT =
            "你是一个查询规划助手。分析用户问题，输出 JSON。\n" +
            "策略：DIRECT(直接检索) / REWRITE(改写查询) / DECOMPOSE(拆分子问题)\n" +
            "最多生成 %d 条检索语句。\n" +
            "输出格式: {\"strategy\":\"DIRECT\",\"queries\":[\"检索语句1\"]}\n" +
            "示例:\n" +
            "  用户: 年假有多少天\n" +
            "  输出: {\"strategy\":\"DIRECT\",\"queries\":[\"年假 天数 带薪年假\"]}\n" +
            "  用户: 请假流程怎么走，需要什么材料\n" +
            "  输出: {\"strategy\":\"DECOMPOSE\",\"queries\":[\"请假申请流程\",\"请假所需材料\"]}\n";

    public Plan plan(String question, String hintStrategy) {
        // 如果指定了策略，直接使用
        if (hintStrategy != null && !hintStrategy.isBlank() && !"AUTO".equalsIgnoreCase(hintStrategy)) {
            return switch (hintStrategy.toUpperCase()) {
                case "REWRITE" -> rewrite(question);
                case "DECOMPOSE" -> decompose(question);
                default -> new Plan("DIRECT", List.of(question));
            };
        }

        // 简单问题直接检索，复杂问题走 LLM 规划
        if (question.length() < 20) {
            return new Plan("DIRECT", List.of(question));
        }
        if (llmClient == null) {
            return new Plan("DIRECT", List.of(question));
        }
        return llmPlan(question);
    }

    private Plan llmPlan(String question) {
        String prompt = String.format(PLANNING_PROMPT, strategyLimit) +
                "\n用户: " + question + "\n输出:";
        try {
            String json = llmClient.generate(prompt, "");
            log.debug("Query plan: {}", json);
            // 提取 JSON
            json = extractJson(json);
            JSONObject obj = JSON.parseObject(json);
            String strategy = obj.getString("strategy");
            JSONArray arr = obj.getJSONArray("queries");
            List<String> queries = new ArrayList<>();
            if (arr != null) {
                int limit = Math.min(arr.size(), strategyLimit);
                for (int i = 0; i < limit; i++) {
                    queries.add(arr.getString(i));
                }
            }
            if (queries.isEmpty()) {
                queries.add(question);
            }
            return new Plan(strategy != null ? strategy : "DIRECT", queries);
        } catch (Exception e) {
            log.warn("Query planning failed, fallback to DIRECT: {}", e.getMessage());
            return new Plan("DIRECT", List.of(question));
        }
    }

    private Plan rewrite(String question) {
        if (llmClient == null) return new Plan("DIRECT", List.of(question));
        String prompt = "将下面的问题改写为更适合知识库检索的关键词短语，只输出改写结果：\n" + question;
        try {
            String rewritten = llmClient.generate(prompt, "");
            if (rewritten != null && !rewritten.isBlank()) {
                return new Plan("REWRITE", List.of(rewritten.trim()));
            }
        } catch (Exception ignored) {}
        return new Plan("DIRECT", List.of(question));
    }

    private Plan decompose(String question) {
        if (llmClient == null) return new Plan("DIRECT", List.of(question));
        String prompt = String.format(
                "将用户问题拆分为最多%d个独立子问题。输出 JSON 数组，每个元素是子问题文本。\n用户: %s\n输出:",
                strategyLimit, question);
        try {
            String json = llmClient.generate(prompt, "");
            json = extractJson(json);
            JSONArray arr = JSON.parseArray(json);
            List<String> queries = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < Math.min(arr.size(), strategyLimit); i++) {
                    queries.add(arr.getString(i));
                }
            }
            if (queries.isEmpty()) queries.add(question);
            return new Plan("DECOMPOSE", queries);
        } catch (Exception e) {
            log.warn("Decompose failed, fallback to DIRECT: {}", e.getMessage());
            return new Plan("DIRECT", List.of(question));
        }
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        int start = text.indexOf('{');
        if (start < 0) start = text.indexOf('[');
        if (start < 0) return "{}";
        int end = text.lastIndexOf('}');
        if (end < 0) end = text.lastIndexOf(']');
        if (end <= start) return "{}";
        return text.substring(start, end + 1);
    }
}
