package com.qiujie.knowledge.service;

import com.qiujie.knowledge.entity.KbUploadSession;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RegisterCommand;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RegisterResult;
import com.qiujie.knowledge.mapper.KbUploadSessionMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.qiujie.spi.UploadSessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link KbUploadCompletionHandler} 单元测试——mock Mapper + DocumentLifecycleService。
 * <p>
 * 验证两条入口（onComplete 回调 / completeFromUpload 补发）与生命周期命令的桥接契约：
 * uploaded 会话字段 → RegisterCommand 映射、无效会话/未合并文件不误调 register。
 * </p>
 */
@DisplayName("知识库上传完成处理器")
@ExtendWith(MockitoExtension.class)
class KbUploadCompletionHandlerUnitTest {

    @Mock
    private KbUploadSessionMapper sessionMapper;
    @Mock
    private KnowledgeDocumentMapper documentMapper;
    @Mock
    private DocumentLifecycleService lifecycle;

    private KbUploadCompletionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new KbUploadCompletionHandler();
        // handler 使用字段注入，测试中手动装配
        ReflectionTestUtils.setField(handler, "sessionMapper", sessionMapper);
        ReflectionTestUtils.setField(handler, "documentMapper", documentMapper);
        ReflectionTestUtils.setField(handler, "lifecycle", lifecycle);
    }

    private KbUploadSession uploadedSession() {
        KbUploadSession s = new KbUploadSession();
        s.setUploadId("upl-001");
        s.setStaffId(9);
        s.setFileName("考勤制度.docx");
        s.setFileExt("docx");
        s.setFileSize(10240L);
        s.setFileHash("abc123");
        s.setChunkCount(2);
        s.setMergedObjectKey("knowledge/2026/08/upl-001.docx");
        return s;
    }

    // ==================== getStoragePrefix ====================

    @Test
    @DisplayName("getStoragePrefix：知识库前缀")
    void storagePrefix() {
        assertEquals("knowledge", handler.getStoragePrefix());
    }

    // ==================== checkDedup ====================

    @Test
    @DisplayName("checkDedup：已有相同文件 → 返回 documentId + name")
    void checkDedup_found() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(42L);
        doc.setName("knowledge/2026/08/old.docx");
        when(documentMapper.selectList(any())).thenReturn(List.of(doc));

        Map<String, Object> result = handler.checkDedup("abc123");

        assertNotNull(result);
        assertEquals(42L, result.get("documentId"));
        assertEquals("knowledge/2026/08/old.docx", result.get("name"));
    }

    @Test
    @DisplayName("checkDedup：无相同文件 → null")
    void checkDedup_notFound() {
        when(documentMapper.selectList(any())).thenReturn(List.of());

        assertNull(handler.checkDedup("nope"));
    }

    // ==================== onComplete（分片上传完成回调） ====================

    @Test
    @DisplayName("onComplete：映射 RegisterCommand 并返回 {documentId, status}")
    void onComplete_mapsCommand() {
        UploadSessionInfo info = new UploadSessionInfo("upl-001", "考勤制度.docx", "docx",
                10240L, "abc123", 9, 2);
        when(lifecycle.register(any(RegisterCommand.class)))
                .thenReturn(new RegisterResult(42L, "UPLOADED"));

        Map<String, Object> result = handler.onComplete("knowledge/2026/08/upl-001.docx", info);

        ArgumentCaptor<RegisterCommand> captor = ArgumentCaptor.forClass(RegisterCommand.class);
        verify(lifecycle).register(captor.capture());
        RegisterCommand cmd = captor.getValue();
        assertEquals("knowledge/2026/08/upl-001.docx", cmd.name());
        assertEquals("考勤制度.docx", cmd.oldName());
        assertEquals("docx", cmd.type());
        assertEquals("abc123", cmd.fileHash());
        assertEquals(10240L, cmd.fileSize());
        assertEquals(9, cmd.staffId());

        assertEquals(42L, result.get("documentId"));
        assertEquals("UPLOADED", result.get("status"));
    }

    // ==================== completeFromUpload（上传后补发摄入） ====================

    @Test
    @DisplayName("completeFromUpload：有效会话 → 透传生命周期登记")
    void completeFromUpload_valid() {
        KbUploadSession session = uploadedSession();
        when(sessionMapper.selectById("upl-001")).thenReturn(session);
        when(lifecycle.register(any(RegisterCommand.class)))
                .thenReturn(new RegisterResult(7L, "UPLOADED"));

        Map<String, Object> result = handler.completeFromUpload("upl-001");

        assertNotNull(result);
        assertEquals(7L, result.get("documentId"));
        assertEquals("UPLOADED", result.get("status"));
        // mergedObjectKey 作为 name 传入
        ArgumentCaptor<RegisterCommand> captor = ArgumentCaptor.forClass(RegisterCommand.class);
        verify(lifecycle).register(captor.capture());
        assertEquals("knowledge/2026/08/upl-001.docx", captor.getValue().name());
    }

    @Test
    @DisplayName("completeFromUpload：uploadId 无效 → null，不调 register")
    void completeFromUpload_invalidId() {
        when(sessionMapper.selectById("nope")).thenReturn(null);

        assertNull(handler.completeFromUpload("nope"));
        verify(lifecycle, never()).register(any());
    }

    @Test
    @DisplayName("completeFromUpload：文件未合并（mergedObjectKey 为 null）→ null")
    void completeFromUpload_notMerged() {
        KbUploadSession session = uploadedSession();
        session.setMergedObjectKey(null);
        when(sessionMapper.selectById("upl-001")).thenReturn(session);

        assertNull(handler.completeFromUpload("upl-001"));
        verify(lifecycle, never()).register(any());
    }
}