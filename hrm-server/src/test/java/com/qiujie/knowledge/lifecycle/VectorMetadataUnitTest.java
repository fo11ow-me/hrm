package com.qiujie.knowledge.lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 向量元数据契约测试：写方（ETL）与读方（检索）共用同一 JSON 形状。
 */
@DisplayName("向量元数据契约")
class VectorMetadataUnitTest {

    @Test
    @DisplayName("完整字段应正确往返")
    void toJson_Then_fromJson_ShouldRoundTrip() {
        String json = VectorMetadata.toJson(42L, 7L, "员工手册.pdf");

        VectorMetadata meta = VectorMetadata.fromJson(json);
        assertEquals(42L, meta.documentId());
        assertEquals(7L, meta.chunkId());
        assertEquals("员工手册.pdf", meta.documentName());
    }

    @Test
    @DisplayName("documentName 为 null 时应序列化为空串")
    void toJson_NullDocumentName_ShouldSerializeAsEmpty() {
        String json = VectorMetadata.toJson(42L, 7L, null);

        assertEquals("", VectorMetadata.fromJson(json).documentName());
    }

    @Test
    @DisplayName("缺字段应回退默认值（兼容存量旧数据，不抛异常）")
    void fromJson_MissingFields_ShouldFallbackToDefaults() {
        // 存量数据只有 documentId/chunkId，缺 documentName
        VectorMetadata legacy = VectorMetadata.fromJson("{\"documentId\":42,\"chunkId\":7}");
        assertEquals(42L, legacy.documentId());
        assertEquals(7L, legacy.chunkId());
        assertEquals("", legacy.documentName());
    }

    @Test
    @DisplayName("非法 JSON 应回退全默认值，不抛异常")
    void fromJson_InvalidJson_ShouldNotThrow() {
        VectorMetadata meta = VectorMetadata.fromJson("{不是JSON");

        assertEquals(0L, meta.documentId());
        assertEquals(0L, meta.chunkId());
        assertEquals("", meta.documentName());
    }

    @Test
    @DisplayName("null 与空串应回退全默认值")
    void fromJson_NullOrBlank_ShouldNotThrow() {
        assertNotNull(VectorMetadata.fromJson(null));
        assertNotNull(VectorMetadata.fromJson("  "));
        assertEquals(0L, VectorMetadata.fromJson(null).documentId());
    }
}
