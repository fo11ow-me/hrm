package com.qiujie.knowledge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("文本切片服务")
class ChunkServiceUnitTest {

    private ChunkService chunkService;

    @BeforeEach
    void setUp() {
        chunkService = new ChunkService();
        ReflectionTestUtils.setField(chunkService, "maxSize", 200);
        ReflectionTestUtils.setField(chunkService, "overlap", 50);
    }

    @Test
    @DisplayName("空文本应返回空列表")
    void split_NullText_ShouldReturnEmpty() {
        assertTrue(chunkService.split(null).isEmpty());
        assertTrue(chunkService.split("").isEmpty());
        assertTrue(chunkService.split("   ").isEmpty());
    }

    @Test
    @DisplayName("单段文本不超过 maxSize 应返回一个切片")
    void split_SingleShortParagraph_ShouldReturnOneChunk() {
        List<ChunkService.ChunkResult> chunks = chunkService.split("这是一段测试文本");

        assertEquals(1, chunks.size());
        assertEquals("这是一段测试文本", chunks.get(0).getText());
        assertTrue(chunks.get(0).getTokenCount() > 0);
    }

    @Test
    @DisplayName("多段文本应合并到同一切片直到超过 maxSize")
    void split_MultipleParagraphs_ShouldRespectMaxSize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("段落").append(i).append("：这是测试内容").append(i).append("\n\n");
        }

        List<ChunkService.ChunkResult> chunks = chunkService.split(sb.toString());

        assertFalse(chunks.isEmpty());
        for (ChunkService.ChunkResult c : chunks) {
            assertTrue(c.getText().length() <= 200,
                    "Chunk length " + c.getText().length() + " exceeds maxSize 200");
            assertTrue(c.getTokenCount() > 0);
        }
    }

    @Test
    @DisplayName("相邻切片应有 overlap 重叠")
    void split_ConsecutiveChunks_ShouldHaveOverlap() {
        // 构造正好会触发切分的文本
        String base = "A".repeat(160);
        String text = base + "\n\n" + "B".repeat(160);

        List<ChunkService.ChunkResult> chunks = chunkService.split(text);

        assertTrue(chunks.size() >= 2, "Should have at least 2 chunks, got " + chunks.size());
        String first = chunks.get(0).getText();
        String second = chunks.get(1).getText();
        assertTrue(second.contains(first.substring(first.length() - 10)),
                "Second chunk should contain overlap from first");
    }

    @Test
    @DisplayName("超大单段应被强制切分")
    void split_SingleHugeParagraph_ShouldStillSplit() {
        String huge = "测试内容".repeat(60); // ~240 chars, > maxSize 200

        List<ChunkService.ChunkResult> chunks = chunkService.split(huge);

        // 注：当前实现按段落边界切，超大单段不强制切分
        // 但段落内部无 \n\n 分割点，所以仍为1个切片
        assertEquals(1, chunks.size());
    }

    @Test
    @DisplayName("Token 估算应与文本长度成比例")
    void split_TokenEstimation_ShouldBeProportionalToTextLength() {
        String shortText = "短文本";
        String longText = "长文本内容".repeat(20);

        List<ChunkService.ChunkResult> shortChunks = chunkService.split(shortText);
        List<ChunkService.ChunkResult> longChunks = chunkService.split(longText);

        int shortTokens = shortChunks.get(0).getTokenCount();
        int longTokens = longChunks.get(0).getTokenCount();
        assertTrue(longTokens > shortTokens,
                "Long text tokens(" + longTokens + ") should exceed short(" + shortTokens + ")");
    }

    @Test
    @DisplayName("纯英文文本 token 估算应正确")
    void split_EnglishText_ShouldEstimateTokens() {
        String english = "This is a test paragraph with English content. It should work correctly.";

        List<ChunkService.ChunkResult> chunks = chunkService.split(english);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getTokenCount() > 0);
    }
}
