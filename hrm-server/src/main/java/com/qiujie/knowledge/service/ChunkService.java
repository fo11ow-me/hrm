package com.qiujie.knowledge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片服务。按段落自然边界分割，兼顾 maxSize 和 overlap。
 */
@Service
public class ChunkService {

    /** 最大切片字符数 */
    @Value("${knowledge.chunk.max-size:1000}")
    private int maxSize;

    /** 相邻切片重叠字符数 */
    @Value("${knowledge.chunk.overlap:100}")
    private int overlap;

    public List<ChunkResult> split(String text) {
        List<ChunkResult> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // 1. 按段落边界切分
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();
        int index = 0;
        String overlapText = "";

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (current.length() + trimmed.length() > maxSize && current.length() > 0) {
                chunks.add(buildChunk(current.toString(), index++));
                // overlap: 保留上一块的结尾部分
                overlapText = current.substring(Math.max(0, current.length() - overlap));
                current = new StringBuilder(overlapText + trimmed + "\n\n");
            } else {
                current.append(trimmed).append("\n\n");
            }
        }

        // 最后一个切片
        if (current.length() > 0) {
            chunks.add(buildChunk(current.toString(), index));
        }

        return chunks;
    }

    private ChunkResult buildChunk(String text, int index) {
        // 估算 token 数：中文约1.5字符/token，英文约4字符/token
        int tokenCount = (int) (text.length() / 2.5);
        return new ChunkResult(text.trim(), tokenCount);
    }

    public static class ChunkResult {
        private final String text;
        private final int tokenCount;

        public ChunkResult(String text, int tokenCount) {
            this.text = text;
            this.tokenCount = tokenCount;
        }

        public String getText() { return text; }
        public int getTokenCount() { return tokenCount; }
    }
}
