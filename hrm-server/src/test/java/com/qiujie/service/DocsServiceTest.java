package com.qiujie.service;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Docs;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.mapper.DocsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DocsService 安全测试
 *
 * @author qiujie
 * @date 2026-06-09
 */
@DisplayName("文档管理服务安全测试")
class DocsServiceTest {

    @Mock
    private DocsMapper docsMapper;

    @InjectMocks
    private DocsService docsService;

    private static final String TEST_FILE_PATH = "target/test-files/";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(docsService, "filePath", TEST_FILE_PATH);
    }

    // ==================== 文件上传安全测试 ====================

    @Test
    @DisplayName("上传空文件应返回错误")
    void testUpload_EmptyFile() throws IOException {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", new byte[0]
        );

        ResponseDTO response = docsService.upload(emptyFile, 1);

        assertNotNull(response);
        assertEquals(600, response.getCode()); // FILE_NOT_EXIST
        verify(docsMapper, never()).insert(any());
    }

    @Test
    @DisplayName("上传超大文件应返回错误")
    void testUpload_ExceedMaxSize() throws IOException {
        // 创建 25MB 的文件 (超过 20MB 限制)
        byte[] largeContent = new byte[25 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", "large.pdf", "application/pdf", largeContent
        );

        ResponseDTO response = docsService.upload(largeFile, 1);

        assertNotNull(response);
        assertEquals(300, response.getCode()); // ERROR
        assertTrue(response.getMessage().contains("文件大小超过限制"));
        verify(docsMapper, never()).insert(any());
    }

    @Test
    @DisplayName("上传不允许的文件类型应返回错误")
    void testUpload_InvalidFileType() throws IOException {
        MockMultipartFile jspFile = new MockMultipartFile(
            "file", "malicious.jsp", "application/octet-stream", "<% Runtime.exec(cmd); %>".getBytes()
        );

        ResponseDTO response = docsService.upload(jspFile, 1);

        assertNotNull(response);
        assertEquals(300, response.getCode()); // ERROR
        assertTrue(response.getMessage().contains("不支持的文件类型"));
        verify(docsMapper, never()).insert(any());
    }

    @Test
    @DisplayName("上传可执行文件应返回错误")
    void testUpload_ExecutableFile() throws IOException {
        MockMultipartFile exeFile = new MockMultipartFile(
            "file", "virus.exe", "application/octet-stream", "MZ".getBytes()
        );

        ResponseDTO response = docsService.upload(exeFile, 1);

        assertNotNull(response);
        assertEquals(300, response.getCode()); // ERROR
        assertTrue(response.getMessage().contains("不支持的文件类型"));
        verify(docsMapper, never()).insert(any());
    }

    @Test
    @DisplayName("上传允许的 PDF 文件应成功")
    void testUpload_ValidPdfFile() throws IOException {
        byte[] pdfContent = "%PDF-1.4 test content".getBytes();
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", "document.pdf", "application/pdf", pdfContent
        );

        when(docsMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(docsMapper.insert(any(Docs.class))).thenReturn(1);

        ResponseDTO response = docsService.upload(pdfFile, 1);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.getMessage().contains("文件上传成功"));
        verify(docsMapper, times(1)).insert(any(Docs.class));
    }

    @Test
    @DisplayName("上传允许的图片文件应成功")
    void testUpload_ValidImageFile() throws IOException {
        byte[] imageContent = new byte[1024];
        MockMultipartFile imageFile = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", imageContent
        );

        when(docsMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(docsMapper.insert(any(Docs.class))).thenReturn(1);

        ResponseDTO response = docsService.upload(imageFile, 1);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        verify(docsMapper, times(1)).insert(any(Docs.class));
    }

    // ==================== MD5 去重测试 ====================

    @Test
    @DisplayName("上传相同文件应去重物理文件(但仍创建数据库记录)")
    void testUpload_MD5Deduplication() throws IOException {
        byte[] content = "same content".getBytes();
        MockMultipartFile file1 = new MockMultipartFile(
            "file", "test1.pdf", "application/pdf", content
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "file", "test2.pdf", "application/pdf", content
        );

        Docs existingDoc = new Docs();
        existingDoc.setName("existing-uuid.pdf");
        existingDoc.setMd5("same-md5");

        // 第一次上传 - 文件不存在
        when(docsMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        when(docsMapper.insert(any(Docs.class))).thenReturn(1);

        ResponseDTO response1 = docsService.upload(file1, 1);
        assertEquals(200, response1.getCode());

        // 重置 mock
        reset(docsMapper);

        // 第二次上传相同内容 - 文件已存在
        when(docsMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Arrays.asList(existingDoc));

        when(docsMapper.insert(any(Docs.class))).thenReturn(1);

        ResponseDTO response2 = docsService.upload(file2, 1);
        assertEquals(200, response2.getCode());

        // 注意: 当前实现会复用物理文件,但仍会在数据库创建新记录
        // 这可能导致 DoS 攻击 (重复创建数据库记录)
        // TODO: 应返回已存在的文件记录,不创建新记录
        verify(docsMapper, times(1)).insert(any());
    }

    // ==================== 文件类型白名单测试 ====================

    @Test
    @DisplayName("文档类型文件应允许上传")
    void testUpload_DocumentTypes() throws IOException {
        String[] allowedTypes = {"pdf", "doc", "docx", "xls", "xlsx", "txt"};

        for (String type : allowedTypes) {
            MockMultipartFile file = new MockMultipartFile(
                "file", "test." + type, "application/octet-stream", "content".getBytes()
            );

            when(docsMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
            when(docsMapper.insert(any(Docs.class))).thenReturn(1);

            ResponseDTO response = docsService.upload(file, 1);

            assertEquals(200, response.getCode(), "文件类型 " + type + " 应允许上传");
        }
    }

    @Test
    @DisplayName("图片类型文件应允许上传")
    void testUpload_ImageTypes() throws IOException {
        String[] allowedTypes = {"jpg", "jpeg", "png", "gif", "bmp", "svg"};

        for (String type : allowedTypes) {
            MockMultipartFile file = new MockMultipartFile(
                "file", "test." + type, "image/" + type, "content".getBytes()
            );

            when(docsMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
            when(docsMapper.insert(any(Docs.class))).thenReturn(1);

            ResponseDTO response = docsService.upload(file, 1);

            assertEquals(200, response.getCode(), "图片类型 " + type + " 应允许上传");
        }
    }

    @Test
    @DisplayName("危险文件类型应拒绝")
    void testUpload_DangerousTypes() throws IOException {
        String[] dangerousTypes = {"jsp", "exe", "sh", "bat", "cmd", "php", "asp", "aspx"};

        for (String type : dangerousTypes) {
            MockMultipartFile file = new MockMultipartFile(
                "file", "malicious." + type, "application/octet-stream", "content".getBytes()
            );

            ResponseDTO response = docsService.upload(file, 1);

            assertEquals(300, response.getCode(), "文件类型 " + type + " 应拒绝上传"); // ERROR
            assertTrue(response.getMessage().contains("不支持的文件类型"));
        }
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("文件大小刚好等于限制应成功")
    void testUpload_ExactMaxSize() throws IOException {
        // 20MB 文件
        byte[] content = new byte[20 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
            "file", "exact.pdf", "application/pdf", content
        );

        when(docsMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(docsMapper.insert(any(Docs.class))).thenReturn(1);

        ResponseDTO response = docsService.upload(file, 1);

        assertEquals(200, response.getCode());
    }

    @Test
    @DisplayName("文件大小超过限制 1 字节应失败")
    void testUpload_ExceedMaxSizeByOneByte() throws IOException {
        // 20MB + 1 字节
        byte[] content = new byte[20 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
            "file", "over.pdf", "application/pdf", content
        );

        ResponseDTO response = docsService.upload(file, 1);

        assertEquals(300, response.getCode()); // ERROR
        assertTrue(response.getMessage().contains("文件大小超过限制"));
    }

    @Test
    @DisplayName("特殊字符文件名应正常处理")
    void testUpload_SpecialCharactersInFilename() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test@#$%^&_文件.pdf", "application/pdf", "content".getBytes()
        );

        when(docsMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(docsMapper.insert(any(Docs.class))).thenReturn(1);

        ResponseDTO response = docsService.upload(file, 1);

        assertEquals(200, response.getCode());
    }

    @Test
    @DisplayName("无扩展名文件应拒绝")
    void testUpload_NoExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file", "noextension", "application/octet-stream", "content".getBytes()
        );

        ResponseDTO response = docsService.upload(file, 1);

        assertEquals(300, response.getCode()); // ERROR
        assertTrue(response.getMessage().contains("不支持的文件类型"));
    }

    @Test
    @DisplayName("大写扩展名应允许")
    void testUpload_UppercaseExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file", "TEST.PDF", "application/pdf", "content".getBytes()
        );

        when(docsMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(docsMapper.insert(any(Docs.class))).thenReturn(1);

        ResponseDTO response = docsService.upload(file, 1);

        assertEquals(200, response.getCode());
    }
}
