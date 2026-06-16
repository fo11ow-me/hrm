package com.qiujie.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ollama 嵌入客户端。用 RestTemplate 调 /v1/embeddings（OpenAI 兼容格式），
 * 作为 Spring AI 1.0.0-M6 PgVectorStore 有 ON CONFLICT bug 的过渡方案。
 */
@Service
public class OllamaEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingClient.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper mapper;

    public OllamaEmbeddingClient(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}") String model) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = baseUrl + "/v1/embeddings";
        this.model = model;
        this.mapper = new ObjectMapper();
        log.info("Ollama embedding client: url={}, model={}", apiUrl, model);
    }

    public List<float[]> embed(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            try {
                Map<String, Object> body = Map.of("model", model, "input", text);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                String response = restTemplate.postForObject(apiUrl, request, String.class);
                JsonNode root = mapper.readTree(response);
                JsonNode dataArray = root.path("data");
                if (!dataArray.isArray() || dataArray.isEmpty()) {
                    throw new RuntimeException("Ollama returned empty embedding (text too long? length=" + text.length() + ")");
                }
                JsonNode embeddingNode = dataArray.get(0).path("embedding");
                float[] vec = new float[embeddingNode.size()];
                for (int i = 0; i < vec.length; i++) {
                    vec[i] = (float) embeddingNode.get(i).asDouble();
                }
                results.add(vec);
            } catch (Exception e) {
                throw new RuntimeException("Ollama embedding failed: " + e.getMessage(), e);
            }
        }
        return results;
    }
}
