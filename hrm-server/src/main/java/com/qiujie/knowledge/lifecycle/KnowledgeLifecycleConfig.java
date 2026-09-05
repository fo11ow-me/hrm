package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.EmbeddingProvider;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.qiujie.knowledge.service.ChunkService;
import com.qiujie.knowledge.service.DocumentParserService;
import com.qiujie.knowledge.service.TextCleanupService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Executor;

/**
 * 文档生命周期模块装配：门面 + 管道 + 清理 + 启动恢复。
 * 三个端口（ChunkVectorStore/ObjectStore/EmbeddingProvider）的生产适配器由组件扫描注册。
 */
@Configuration
public class KnowledgeLifecycleConfig {

    @Bean
    public IngestionPipeline ingestionPipeline(KnowledgeDocumentMapper documentMapper,
                                               IngestionJobMapper jobMapper,
                                               ChunkVectorStore chunkVectorStore,
                                               ObjectStore objectStore,
                                               EmbeddingProvider embeddingProvider,
                                               DocumentParserService parserService,
                                               TextCleanupService textCleanupService,
                                               ChunkService chunkService) {
        return new IngestionPipeline(documentMapper, jobMapper, chunkVectorStore, objectStore,
                embeddingProvider, parserService, textCleanupService, chunkService);
    }

    @Bean
    public DocumentPurgeHandler documentPurgeHandler(KnowledgeDocumentMapper documentMapper,
                                                     ChunkVectorStore chunkVectorStore,
                                                     ObjectStore objectStore) {
        return new DocumentPurgeHandler(documentMapper, chunkVectorStore, objectStore);
    }

    @Bean
    public DocumentLifecycleService documentLifecycleService(KnowledgeDocumentMapper documentMapper,
                                                             IngestionJobMapper jobMapper,
                                                             TransactionTemplate transactionTemplate,
                                                             @Qualifier("fileTaskExecutor") Executor ingestExecutor,
                                                             IngestionPipeline pipeline,
                                                             DocumentPurgeHandler purgeHandler) {
        return new DocumentLifecycleService(documentMapper, jobMapper, transactionTemplate,
                ingestExecutor, pipeline, purgeHandler);
    }

    /** 启动恢复：仅在知识库启用时注册（四步恢复覆盖三个崩溃窗口）。 */
    @Bean
    @ConditionalOnExpression("${knowledge.enabled:false}")
    public StartupRecovery startupRecovery(KnowledgeDocumentMapper documentMapper,
                                           IngestionJobMapper jobMapper,
                                           IngestionPipeline pipeline,
                                           DocumentPurgeHandler purgeHandler,
                                           @Qualifier("fileTaskExecutor") Executor executor) {
        return new StartupRecovery(documentMapper, jobMapper, pipeline, purgeHandler, executor);
    }
}
