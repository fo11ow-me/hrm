package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.IngestionJob;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.DeleteCommand;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.DeleteResult;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RegisterCommand;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RegisterResult;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RetryCommand;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RetryResult;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore.VectorDraft;
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
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文档生命周期边界测试：同步 executor + 内存三端口 + mock 的 MySQL mapper。
 * 全链路零数据库：上传完成 → ETL → READY 的状态机行为通过公共接口可观察。
 */
@DisplayName("文档生命周期门面")
class DocumentLifecycleServiceUnitTest {

    private static final long DOC_ID = 42L;

    private DocumentLifecycleService service;
    private KnowledgeDocumentMapper documentMapper;
    private IngestionJobMapper jobMapper;
    private InMemoryChunkVectorStore chunkVectorStore;
    private InMemoryObjectStore objectStore;
    private KnowledgeDocument insertedDoc;
    private IngestionJob insertedJob;

    @BeforeEach
    void setUp() {
        documentMapper = mock(KnowledgeDocumentMapper.class);
        jobMapper = mock(IngestionJobMapper.class);
        chunkVectorStore = new InMemoryChunkVectorStore();
        objectStore = new InMemoryObjectStore();

        // 纯函数件真实注入
        ChunkService chunkService = new ChunkService();
        ReflectionTestUtils.setField(chunkService, "maxSize", 200);
        ReflectionTestUtils.setField(chunkService, "overlap", 50);
        IngestionPipeline pipeline = new IngestionPipeline(documentMapper, jobMapper, chunkVectorStore,
                objectStore, new FixedEmbeddingProvider(),
                new DocumentParserService(), new TextCleanupService(), chunkService);

        // TransactionTemplate mock：回调直跑（无真实事务上下文，异步触发走"立即执行"分支）
        TransactionTemplate txTemplate = mock(TransactionTemplate.class);
        when(txTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });

        DocumentPurgeHandler purgeHandler = new DocumentPurgeHandler(documentMapper, chunkVectorStore, objectStore);
        service = new DocumentLifecycleService(documentMapper, jobMapper, txTemplate, Runnable::run, pipeline, purgeHandler);

        when(documentMapper.insert(any(KnowledgeDocument.class))).thenAnswer(inv -> {
            insertedDoc = inv.getArgument(0);
            insertedDoc.setId(DOC_ID);
            return 1;
        });
        when(documentMapper.selectById(DOC_ID)).thenAnswer(inv -> insertedDoc);
        when(documentMapper.claimForProcessing(DOC_ID)).thenReturn(1);
        when(documentMapper.completeProcessing(eq(DOC_ID), anyString(), anyInt())).thenReturn(1);
        when(documentMapper.markFailed(eq(DOC_ID), anyString())).thenReturn(1);
        when(documentMapper.markDeleted(DOC_ID)).thenReturn(1);
        when(jobMapper.insert(any(IngestionJob.class))).thenAnswer(inv -> {
            insertedJob = inv.getArgument(0);
            insertedJob.setId(1L);
            return 1;
        });
        when(jobMapper.updateById(any(IngestionJob.class))).thenAnswer(inv -> {
            insertedJob = inv.getArgument(0);
            return 1;
        });
    }

    @Test
    @DisplayName("上传完成登记后应完成 ETL 并置 READY")
    void register_ShouldIngestDocumentToReady() {
        objectStore.put("knowledge/5/x/手册.txt",
                "第一段内容\n\n第二段内容".getBytes(StandardCharsets.UTF_8));

        RegisterResult result = service.register(new RegisterCommand(
                "knowledge/5/x/手册.txt", "员工手册.txt", "txt", "hash1", 100L, 5));

        // 提交结果：文档 ID + 提交时状态 UPLOADED（ETL 异步）
        assertEquals(DOC_ID, result.documentId());
        assertEquals("UPLOADED", result.status());

        // 同步 executor 下 ETL 已执行完：切片持久化
        assertEquals(1, chunkVectorStore.listChunks(DOC_ID).size());

        // 契约修复：向量元数据携带文档名（写读同源）
        List<VectorDraft> vectors = chunkVectorStore.vectors();
        assertEquals(1, vectors.size());
        assertEquals("员工手册.txt", vectors.get(0).documentName());
        assertEquals(DOC_ID, vectors.get(0).documentId());

        // 镜像已同步（关键词检索的 JOIN 来源）
        assertEquals("READY", chunkVectorStore.mirror(DOC_ID).getStatus());

        // 文档结算 READY + 作业 SUCCEEDED
        verify(documentMapper).completeProcessing(eq(DOC_ID), anyString(), eq(1));
        assertEquals("SUCCEEDED", insertedJob.getStatus());
        assertNotNull(insertedJob.getFinishedAt());
    }

    @Test
    @DisplayName("ETL 失败应立即置文档 FAILED 并写入原因（不再卡 PROCESSING）")
    void register_EtlFailure_ShouldMarkDocumentFailedWithReason() {
        // 对象存储中没有该文件 → ETL 抛异常
        service.register(new RegisterCommand(
                "knowledge/5/x/缺失.txt", "员工手册.txt", "txt", "hash2", 100L, 5));

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(documentMapper).markFailed(eq(DOC_ID), reason.capture());
        assertFalse(reason.getValue().isBlank());
        assertEquals("FAILED", insertedJob.getStatus());
        assertNotNull(insertedJob.getLastError());
        // ETL 未完成：无切片、无向量、无镜像
        assertTrue(chunkVectorStore.listChunks(DOC_ID).isEmpty());
        assertTrue(chunkVectorStore.vectors().isEmpty());
        assertNull(chunkVectorStore.mirror(DOC_ID));
        // 失败不能结算 READY
        verify(documentMapper, never()).completeProcessing(eq(DOC_ID), anyString(), anyInt());
    }

    @Test
    @DisplayName("READY 文档重试应被拒绝且不触发管道")
    void retry_ReadyDocument_ShouldReject() {
        insertedDoc = new KnowledgeDocument().setId(DOC_ID)
                .setName("knowledge/5/x/手册.txt").setOldName("员工手册.txt")
                .setType("txt").setStaffId(5);
        insertedDoc.setStatus("READY");

        RetryResult result = service.retry(new RetryCommand(DOC_ID));

        assertFalse(result.accepted());
        assertFalse(result.reason().isBlank());
        verify(documentMapper, never()).claimForProcessing(any());
    }

    @Test
    @DisplayName("FAILED 文档重试应接受并重新执行 ETL 至 READY")
    void retry_FailedDocument_ShouldRerunEtl() {
        insertedDoc = new KnowledgeDocument().setId(DOC_ID)
                .setName("knowledge/5/x/手册.txt").setOldName("员工手册.txt")
                .setType("txt").setStaffId(5);
        insertedDoc.setStatus("FAILED");
        objectStore.put("knowledge/5/x/手册.txt",
                "重试后内容".getBytes(StandardCharsets.UTF_8));

        RetryResult result = service.retry(new RetryCommand(DOC_ID));

        assertTrue(result.accepted());
        assertEquals("READY", chunkVectorStore.mirror(DOC_ID).getStatus());
        verify(documentMapper).completeProcessing(eq(DOC_ID), anyString(), anyInt());
    }

    @Test
    @DisplayName("不存在的文档重试应被拒绝")
    void retry_MissingDocument_ShouldReject() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(null);

        RetryResult result = service.retry(new RetryCommand(DOC_ID));

        assertFalse(result.accepted());
        assertFalse(result.reason().isBlank());
        verify(documentMapper, never()).claimForProcessing(any());
    }

    @Test
    @DisplayName("CAS 认领失败（并发触发）应静默跳过，不重复摄入")
    void register_ClaimLost_ShouldSkipSilently() {
        when(documentMapper.claimForProcessing(DOC_ID)).thenReturn(0);
        objectStore.put("knowledge/5/x/手册.txt", "内容".getBytes(StandardCharsets.UTF_8));

        service.register(new RegisterCommand(
                "knowledge/5/x/手册.txt", "员工手册.txt", "txt", "hash3", 100L, 5));

        verify(jobMapper, never()).insert(any(IngestionJob.class));
        assertTrue(chunkVectorStore.vectors().isEmpty());
        assertNull(chunkVectorStore.mirror(DOC_ID));
        verify(documentMapper, never()).completeProcessing(eq(DOC_ID), anyString(), anyInt());
    }

    @Test
    @DisplayName("ETL 执行中文档被删应取消作业并清理残留，不复活已删文档")
    void register_DeletedDuringEtl_ShouldCancelJobAndCleanup() {
        when(documentMapper.completeProcessing(eq(DOC_ID), anyString(), anyInt())).thenReturn(0);
        objectStore.put("knowledge/5/x/手册.txt",
                "第一段\n\n第二段".getBytes(StandardCharsets.UTF_8));

        service.register(new RegisterCommand(
                "knowledge/5/x/手册.txt", "员工手册.txt", "txt", "hash4", 100L, 5));

        assertEquals("CANCELLED", insertedJob.getStatus());
        assertTrue(chunkVectorStore.vectors().isEmpty());
        assertTrue(chunkVectorStore.listChunks(DOC_ID).isEmpty());
        assertTrue(chunkVectorStore.mirrorRemoved(DOC_ID));
    }

    @Test
    @DisplayName("删除应级联清理物理文件、切片、向量、镜像并作废在途作业")
    void delete_ShouldCascadeCleanup() {
        String key = "knowledge/5/x/手册.txt";
        objectStore.put(key, "第一段\n\n第二段".getBytes(StandardCharsets.UTF_8));
        service.register(new RegisterCommand(key, "员工手册.txt", "txt", "hash5", 100L, 5));

        DeleteResult result = service.delete(new DeleteCommand(DOC_ID));

        assertFalse(result.alreadyDeleted());
        assertFalse(objectStore.has(key));                      // 物理文件已删
        assertTrue(chunkVectorStore.listChunks(DOC_ID).isEmpty());
        assertTrue(chunkVectorStore.vectors().isEmpty());
        assertTrue(chunkVectorStore.mirrorRemoved(DOC_ID));
        verify(jobMapper).cancelActiveJobs(DOC_ID);             // 在途作业已作废
    }

    @Test
    @DisplayName("重复删除应幂等成功，不触发物理清理")
    void delete_AlreadyDeleted_ShouldBeIdempotent() {
        insertedDoc = new KnowledgeDocument().setId(DOC_ID)
                .setName("knowledge/5/x/手册.txt").setOldName("员工手册.txt").setStaffId(5);
        when(documentMapper.markDeleted(DOC_ID)).thenReturn(0);

        DeleteResult result = service.delete(new DeleteCommand(DOC_ID));

        assertTrue(result.alreadyDeleted());
        verify(documentMapper, never()).countLiveByFileName(anyString(), any());
        verify(jobMapper, never()).cancelActiveJobs(any());
    }

    @Test
    @DisplayName("物理文件被其他存活文档共享时应保留文件，但清理检索产物")
    void delete_SharedFile_ShouldKeepObjectButCleanupChunks() {
        String key = "knowledge/5/x/手册.txt";
        insertedDoc = new KnowledgeDocument().setId(DOC_ID)
                .setName(key).setOldName("员工手册.txt").setStaffId(5);
        objectStore.put(key, "内容".getBytes(StandardCharsets.UTF_8));
        when(documentMapper.countLiveByFileName(key, DOC_ID)).thenReturn(1L);

        DeleteResult result = service.delete(new DeleteCommand(DOC_ID));

        assertFalse(result.alreadyDeleted());
        assertTrue(objectStore.has(key));                       // 引用计数 > 0，物理文件保留
        assertTrue(chunkVectorStore.mirrorRemoved(DOC_ID));
    }
}
