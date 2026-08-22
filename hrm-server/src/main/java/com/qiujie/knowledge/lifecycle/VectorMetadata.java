package com.qiujie.knowledge.lifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * vector_store.metadata 的 JSON 形状唯一事实来源。
 * <p>
 * 写入方（ETL 管道）经 {@link #toJson} 落库，读取方（检索服务）经 {@link #fromJson} 解析，
 * 禁止任何一侧自行拼接/拆解 JSON —— 修复写方只写 documentId/chunkId、
 * 读方却读 documentName 导致的文档名恒空契约 bug。
 */
public record VectorMetadata(Long documentId, Long chunkId, String documentName) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 序列化为可写 jsonb 的 JSON 字符串。
     *
     * @param documentName 显示用文档名，取 kb_document.old_name 快照；null 时写空串
     */
    public static String toJson(long documentId, long chunkId, String documentName) {
        try {
            return MAPPER.writeValueAsString(new VectorMetadata(
                    documentId, chunkId, documentName == null ? "" : documentName));
        } catch (Exception e) {
            throw new IllegalStateException("向量元数据序列化失败", e);
        }
    }

    /**
     * 容错解析：非法 JSON 或缺失字段时对应字段回退默认值（0/""），绝不抛异常，
     * 兼容历史存量数据（早期行没有 documentName 键）。
     */
    public static VectorMetadata fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new VectorMetadata(0L, 0L, "");
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return new VectorMetadata(
                    node.path("documentId").asLong(0),
                    node.path("chunkId").asLong(0),
                    node.path("documentName").asText(""));
        } catch (Exception e) {
            return new VectorMetadata(0L, 0L, "");
        }
    }
}
