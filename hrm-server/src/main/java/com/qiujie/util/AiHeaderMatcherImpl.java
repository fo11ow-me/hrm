package com.qiujie.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 兜底：将 Excel 列名动态匹配到实体字段。
 * <p>
 * 仅在 ColumnMappingRegistry 未命中时调用，结果会缓存避免重复 LLM 调用。
 *
 * @author qiujie
 */
@Component
public class AiHeaderMatcherImpl implements FlexibleExcelImporter.AiHeaderMatcher {

    private static final Logger log = LoggerFactory.getLogger(AiHeaderMatcherImpl.class);

    @Value("${assistant.provider.base-url:}")
    private String baseUrl;

    @Value("${assistant.provider.api-key:}")
    private String apiKey;

    @Value("${assistant.provider.model:}")
    private String model;

    private RestTemplate restTemplate;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public AiHeaderMatcherImpl(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 调用 LLM 将未知的 Excel 列名匹配到可用字段。
     *
     * @param headerText      Excel 表头文字
     * @param availableFields 实体可用字段列表
     * @return 匹配的字段名，或 null
     */
    @Override
    public String match(String headerText, List<String> availableFields) {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
            return null;
        }
        String cacheKey = headerText + "||" + String.join(",", availableFields);
        return cache.computeIfAbsent(cacheKey, k -> doMatch(headerText, availableFields));
    }

    private String doMatch(String headerText, List<String> availableFields) {
        String prompt = "你是Excel列名匹配专家。将Excel列名映射到最合适的Java字段名。只返回字段名，不要任何解释。\n"
                + "可用字段: " + String.join(", ", availableFields) + "\n"
                + "Excel列名: \"" + headerText + "\"\n"
                + "匹配的字段名:";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", 0.0);
            body.put("max_tokens", 20);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            body.put("messages", List.of(userMsg));

            String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";
            @SuppressWarnings("rawtypes")
            Map response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            if (response == null) return null;

            String content = extractContent(response);
            if (content != null) {
                String matched = content.trim();
                // 验证匹配结果在可用字段列表中
                if (availableFields.contains(matched)) {
                    log.info("AI 列名匹配: {} → {}", headerText, matched);
                    return matched;
                }
                // 尝试模糊匹配：去掉 AI 可能返回的多余字符
                for (String field : availableFields) {
                    if (matched.contains(field)) {
                        log.info("AI 列名匹配(模糊): {} → {}", headerText, field);
                        return field;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI 列名匹配失败: {}", headerText, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return null;
            return (String) message.get("content");
        } catch (Exception e) {
            return null;
        }
    }
}
