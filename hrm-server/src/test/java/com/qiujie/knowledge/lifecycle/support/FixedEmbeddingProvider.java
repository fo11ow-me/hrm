package com.qiujie.knowledge.lifecycle.support;

import com.qiujie.knowledge.lifecycle.port.EmbeddingProvider;

import java.util.List;

/**
 * EmbeddingProvider 固定向量适配器：按文本 hashCode 生成确定性向量。
 * 维度可配置（pgvector 列要求 1024 维时传入 1024）。
 */
public class FixedEmbeddingProvider implements EmbeddingProvider {

    private final int dimension;

    public FixedEmbeddingProvider() {
        this(1);
    }

    public FixedEmbeddingProvider(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public List<float[]> embedTexts(List<String> texts) {
        return texts.stream().map(this::fixedVector).toList();
    }

    private float[] fixedVector(String text) {
        float[] vec = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vec[i] = text.hashCode() % 97 + i;
        }
        return vec;
    }
}
