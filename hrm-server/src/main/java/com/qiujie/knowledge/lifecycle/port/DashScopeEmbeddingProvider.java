package com.qiujie.knowledge.lifecycle.port;

import com.qiujie.knowledge.service.DashScopeEmbeddingClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * EmbeddingProvider 生产适配器：委托现有 DashScopeEmbeddingClient（批量 + null 容错契约保留）。
 */
@Component
public class DashScopeEmbeddingProvider implements EmbeddingProvider {

    private final DashScopeEmbeddingClient client;

    public DashScopeEmbeddingProvider(DashScopeEmbeddingClient client) {
        this.client = client;
    }

    @Override
    public List<float[]> embedTexts(List<String> texts) {
        return client.embed(texts);
    }
}
