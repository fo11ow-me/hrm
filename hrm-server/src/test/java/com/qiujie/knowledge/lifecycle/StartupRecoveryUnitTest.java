package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.lifecycle.support.FixedEmbeddingProvider;
import com.qiujie.knowledge.lifecycle.support.InMemoryChunkVectorStore;
import com.qiujie.knowledge.lifecycle.support.InMemoryObjectStore;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.qiujie.knowledge.service.ChunkService;
import com.qiujie.knowledge.service.DocumentParserService;
import com.qiujie.knowledge.service.TextCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 启动恢复边界测试：四步恢复覆盖三个崩溃窗口（零数据库）。
 */
@DisplayName("启动恢复")
class StartupRecoveryUnitTest {

    private static final long UPLOADED_DOC_ID = 7L;
    private static final long DELETED_DOC_ID = 8L;

    private StartupRecovery recovery;
    private KnowledgeDocumentMapper documentMapper;
    private IngestionJobMapper jobMapper;
    private InMemoryChunkVectorStore chunkVectorStore;
    private InMemoryObjectStore objectStore;
    private KnowledgeDocument uploadedDoc;

    @BeforeEach
    void setUp() {
        documentMapper = mock(KnowledgeDocumentMapper.class);
        jobMapper = mock(IngestionJobMapper.class);
        chunkVectorStore = new InMemoryChunkVectorStore();
        objectStore = new InMemoryObjectStore();

        ChunkService chunkService = new ChunkService();
        ReflectionTestUtils.setField(chunkService, "maxSize", 200);
        ReflectionTestUtils.setField(chunkService, "overlap", 50);
        IngestionPipeline pipeline = new IngestionPipeline(documentMapper, jobMapper, chunkVectorStore,
                objectStore, new FixedEmbeddingProvider(),
                new DocumentParserService(), new TextCleanupService(), chunkService);
        DocumentPurgeHandler purgeHandler = new DocumentPurgeHandler(documentMapper, chunkVectorStore, objectStore);

        // 与生产装配一致复用文件任务线程池语义；同步执行便于断言
        Executor syncExecutor = Runnable::run;
        recovery = new StartupRecovery(documentMapper, jobMapper, pipeline, purgeHandler, syncExecutor);
    }

    @Test
    @DisplayName("四步恢复：收尾作业、标失败、续跑 UPLOADED、重清理已删孤儿")
    void run_ShouldCoverAllCrashWindows() {
        // 步骤1：遗留 RUNNING 作业
        when(jobMapper.markStaleRunningAsFailed(anyString())).thenReturn(2);
        // 步骤2：PROCESSING → FAILED（1 个）
        when(documentMapper.markStaleProcessingAsFailed(anyString())).thenReturn(1);
        // 步骤3：UPLOADED 续跑
        uploadedDoc = new KnowledgeDocument().setId(UPLOADED_DOC_ID)
                .setName("knowledge/5/x/续跑.txt").setOldName("续跑手册.txt")
                .setType("txt").setStaffId(5);
        uploadedDoc.setStatus("UPLOADED");
        when(documentMapper.selectLiveUploaded()).thenReturn(List.of(uploadedDoc));
        when(documentMapper.selectById(UPLOADED_DOC_ID)).thenReturn(uploadedDoc);
        when(documentMapper.claimForProcessing(UPLOADED_DOC_ID)).thenReturn(1);
        when(documentMapper.completeProcessing(eq(UPLOADED_DOC_ID), anyString(), anyInt())).thenReturn(1);
        objectStore.put("knowledge/5/x/续跑.txt", "续跑内容".getBytes(StandardCharsets.UTF_8));
        // 步骤4：已删孤儿重 purge
        KnowledgeDocument deletedDoc = new KnowledgeDocument().setId(DELETED_DOC_ID)
                .setName("knowledge/5/x/孤儿.txt").setOldName("孤儿.txt").setStaffId(5);
        when(documentMapper.selectDeleted()).thenReturn(List.of(deletedDoc));
        objectStore.put("knowledge/5/x/孤儿.txt", "孤儿内容".getBytes(StandardCharsets.UTF_8));

        recovery.run(null);

        // 步骤1：作业收尾
        verify(jobMapper).markStaleRunningAsFailed(anyString());
        // 步骤2：PROCESSING 标失败
        verify(documentMapper).markStaleProcessingAsFailed(anyString());
        // 步骤3：UPLOADED 续跑至 READY（镜像已同步）
        assertEquals("READY", chunkVectorStore.mirror(UPLOADED_DOC_ID).getStatus());
        // 步骤4：孤儿物理文件与检索产物已清理
        assertFalse(objectStore.has("knowledge/5/x/孤儿.txt"));
        assertTrue(chunkVectorStore.mirrorRemoved(DELETED_DOC_ID));
    }
}
