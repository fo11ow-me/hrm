package com.qiujie.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleAssistantLlmClient implements AssistantLlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAssistantLlmClient.class);

    private final AssistantProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleAssistantLlmClient(AssistantProperties properties,
                                              RestTemplateBuilder restTemplateBuilder,
                                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public String generate(String question, String toolContext) {
        AssistantProperties.Provider provider = properties.getProvider();
        if (!properties.isEnabled()
                || provider == null
                || !StringUtils.hasText(provider.getBaseUrl())
                || !StringUtils.hasText(provider.getApiKey())
                || !StringUtils.hasText(provider.getModel())) {
            return "";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(provider.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", provider.getModel());
        body.put("temperature", 0.2);
        body.put("messages", buildMessages(question, toolContext));

        ResponseEntity<String> response = restTemplate.postForEntity(
                resolveEndpoint(provider.getBaseUrl()),
                new HttpEntity<>(body, headers),
                String.class
        );
        return extractContent(response.getBody());
    }

    private List<Map<String, String>> buildMessages(String question, String toolContext) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", "你是 HRM 系统的员工自助助手。只能基于系统提供的工具结果回答，不能编造数据，不能提供跨员工或跨部门数据。");
        messages.add(system);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", "用户问题：" + question + "\n系统工具结果：" + toolContext);
        messages.add(user);
        return messages;
    }

    private String resolveEndpoint(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        return normalized + "/chat/completions";
    }

    private String extractContent(String body) {
        if (!StringUtils.hasText(body)) {
            log.warn("LLM response body is empty");
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);

            // 检查 API 错误响应
            if (root.has("error")) {
                JsonNode error = root.path("error");
                String errorType = error.path("type").asText("unknown");
                String errorMsg = error.path("message").asText("Unknown error");

                log.error("LLM API error: type={}, message={}", errorType, errorMsg);

                // 根据错误类型返回不同的提示
                if ("insufficient_quota".equals(errorType) || "rate_limit_exceeded".equals(errorType)) {
                    throw new ServiceException(500, "AI 服务配额不足或请求过于频繁，请稍后再试");
                } else if ("invalid_api_key".equals(errorType)) {
                    throw new ServiceException(500, "AI 服务配置错误，请联系管理员");
                } else {
                    throw new ServiceException(500, "AI 服务暂时不可用: " + errorMsg);
                }
            }

            // 提取正常响应内容
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || !choices.isArray() || choices.size() == 0) {
                log.warn("LLM response missing choices field: {}", body);
                return "";
            }

            JsonNode content = choices.path(0).path("message").path("content");
            if (content.isMissingNode()) {
                log.warn("LLM response missing content field: {}", body);
                return "";
            }

            String result = content.asText("");
            log.debug("LLM generated {} characters", result.length());
            return result;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response: {}", body, e);
            throw new ServiceException(500, "AI 服务响应格式错误");
        } catch (ServiceException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing LLM response", e);
            throw new ServiceException(500, "AI 服务响应处理失败");
        }
    }
}
