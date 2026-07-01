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
 * 阿里百炼 DashScope 嵌入客户端（OpenAI 兼容模式）。
 *
 * @author quuj
 */
@Service
public class DashScopeEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeEmbeddingClient.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper mapper;

    /**
     * @param baseUrl DashScope OpenAI 兼容接口地址，默认北京区域
     * @param apiKey  百炼 API Key，通过 Bearer Auth 注入每个请求
     * @param model   嵌入模型名称，默认 text-embedding-v4（Qwen3-Embedding）
     */
    public DashScopeEmbeddingClient(
            @Value("${knowledge.embedding.base-url:https://dashscope.aliyuncs.com/compatible-mode}") String baseUrl,
            @Value("${knowledge.embedding.api-key:}") String apiKey,
            @Value("${knowledge.embedding.model:text-embedding-v4}") String model) {
        // RestTemplate 是线程安全的，构造时创建一次，后续复用
        this.restTemplate = new RestTemplate();
        // 拼接完整 API 地址：{baseUrl}/v1/embeddings
        this.apiUrl = baseUrl + "/v1/embeddings";
        this.model = model;
        // Jackson ObjectMapper 线程安全，复用
        this.mapper = new ObjectMapper();
        // apiKey 为空时不注入拦截器，允许匿名访问（如本地 Ollama 场景）
        if (apiKey != null && !apiKey.isBlank()) {
            // 通过拦截器注入 Authorization: Bearer {apiKey}，无需每次手动设置
            this.restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().setBearerAuth(apiKey);
                return execution.execute(request, body);
            });
        }
        log.info("DashScope embedding: url={}, model={}", apiUrl, model);
    }

    /**
     * 调用 DashScope OpenAI 兼容接口，将文本列表逐一转为浮点向量。
     * <p>
     * 接口文档：POST {baseUrl}/v1/embeddings，入参 model + input，出参 data[0].embedding。
     * <p>
     * 注意：当前为逐条调用而非批量（单次只传一个 text），吞吐量受限于网络 RTT。
     * 文档量较大时可改用 input 传数组批量提交，百炼单次支持最多 10 条。
     *
     * @param texts 待向量化的文本列表
     * @return 等长的向量列表，每个元素为 float[]
     */
    public List<float[]> embed(List<String> texts) {
        // 结果列表，与输入等长一一对应
        List<float[]> results = new ArrayList<>();
        // 逐条调用 DashScope API（非批量），保证错误隔离：一条失败不影响其他
        for (String text : texts) {
            try {
                // 构造 OpenAI 兼容请求体：{"model":"text-embedding-v4","input":"..."}
                Map<String, Object> body = Map.of("model", model, "input", text);
                // 设置 HTTP Content-Type = application/json
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // 包装请求体和请求头
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                // 同步 POST 请求，返回 JSON 字符串
                String response = restTemplate.postForObject(apiUrl, request, String.class);
                // Jackson 解析 JSON
                JsonNode root = mapper.readTree(response);
                // 取 data 数组节点
                JsonNode dataArray = root.path("data");
                // 空数组防御：text 过长或模型不支持时可能返回空
                if (!dataArray.isArray() || dataArray.isEmpty()) {
                    throw new RuntimeException(
                        "DashScope returned empty embedding (text too long? length="
                        + text.length() + ")");
                }
                // 取 data[0].embedding 数组
                JsonNode embeddingNode = dataArray.get(0).path("embedding");
                // 将 JSON 数组转为 Java float[]，PGvector 需要 float[] 而非 double[]
                float[] vec = new float[embeddingNode.size()];
                for (int i = 0; i < vec.length; i++) {
                    vec[i] = (float) embeddingNode.get(i).asDouble();
                }
                results.add(vec);
            } catch (Exception e) {
                // 包装为 RuntimeException 向上抛出，触发 @Retryable 重试
                throw new RuntimeException("DashScope embedding failed: " + e.getMessage(), e);
            }
        }
        return results;
    }
}
