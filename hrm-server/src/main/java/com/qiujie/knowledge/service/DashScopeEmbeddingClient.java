package com.qiujie.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里百炼 DashScope 嵌入客户端（原生 API，支持批量）。
 * <p>
 * 原生 API 单次最多 100 条文本，实际使用 batchSize=10 避免单次请求过大。
 *
 * @author quuj
 */
@Service
public class DashScopeEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeEmbeddingClient.class);

    /** 每批次提交的文本数 */
    private static final int BATCH_SIZE = 10;

    private final RestTemplate restTemplate;
    private final String nativeApiUrl;
    private final String model;
    private final ObjectMapper mapper;

    /**
     * @param baseUrl DashScope 兼容接口地址，从中提取 host 构建原生 API URL
     * @param apiKey  百炼 API Key
     * @param model   嵌入模型名称
     */
    public DashScopeEmbeddingClient(
            @Value("${knowledge.embedding.base-url:https://dashscope.aliyuncs.com/compatible-mode}") String baseUrl,
            @Value("${knowledge.embedding.api-key:}") String apiKey,
            @Value("${knowledge.embedding.model:text-embedding-v4}") String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
        // 从兼容模式 baseUrl 推导原生 API 地址
        String host = baseUrl.replace("/compatible-mode", "");
        this.nativeApiUrl = host + "/api/v1/services/embeddings/text-embedding/text-embedding";
        this.model = model;
        this.mapper = new ObjectMapper();
        if (apiKey != null && !apiKey.isBlank()) {
            this.restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().setBearerAuth(apiKey);
                return execution.execute(request, body);
            });
        }
        log.info("DashScope embedding (native API): url={}, model={}, batchSize={}",
                nativeApiUrl, model, BATCH_SIZE);
    }

    /**
     * 使用 DashScope 原生 API 批量向量化文本。
     * <p>
     * 请求格式：{"model":"...","input":{"texts":[...]}}
     * 响应格式：{"output":{"embeddings":[{"text_index":0,"embedding":[...]},...]}}
     *
     * @param texts 待向量化的文本列表
     * @return 等长的向量列表，失败条目为 null
     */
    public List<float[]> embed(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, texts.size());
            List<String> batch = new ArrayList<>(texts.subList(start, end));
            try {
                Map<String, Object> input = new HashMap<>();
                input.put("texts", batch);
                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("input", input);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                String response = restTemplate.postForObject(nativeApiUrl, request, String.class);
                JsonNode root = mapper.readTree(response);
                JsonNode embeddings = root.path("output").path("embeddings");

                for (int i = 0; i < batch.size(); i++) {
                    float[] vec = null;
                    if (i < embeddings.size()) {
                        JsonNode emb = embeddings.get(i).path("embedding");
                        if (emb.isArray() && emb.size() > 0) {
                            vec = new float[emb.size()];
                            for (int j = 0; j < vec.length; j++) {
                                vec[j] = (float) emb.get(j).asDouble();
                            }
                        }
                    }
                    results.add(vec);
                }
            } catch (Exception e) {
                log.warn("DashScope batch embedding failed: start={}, size={}, msg={}",
                        start, batch.size(), e.getMessage());
                for (int i = 0; i < batch.size(); i++) {
                    results.add(null);
                }
            }
        }
        log.info("Embedded {}/{} texts successfully ({} batches)",
                results.stream().filter(v -> v != null).count(), texts.size(),
                (int) Math.ceil((double) texts.size() / BATCH_SIZE));
        return results;
    }
}
