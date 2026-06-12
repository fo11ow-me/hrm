package com.qiujie.service;

import cn.hutool.core.io.FileUtil;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Docs;
import com.qiujie.util.StorageCompressor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocsService 压缩功能集成测试。
 *
 * @author qiujie
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DocsServiceCompressionTest {

    @Autowired
    private DocsService docsService;

    @Value("${file-path}")
    private String filePath;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // 确保测试目录存在
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Test
    @DisplayName("上传可压缩 CSV 文件 → 压缩存储 → DB 标记 compressed=1")
    void uploadCsvShouldCompress() throws Exception {
        String csvContent = "name,age,city\n" + ("Alice,30,Beijing\n").repeat(200);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        ResponseDTO response = docsService.upload(file, 1);
        assertThat(response.getCode()).isEqualTo(200);

        Docs docs = (Docs) response.getData();
        assertThat(docs.getCompressed()).isEqualTo(1);
        assertThat(docs.getStoredSize()).isLessThan(csvContent.getBytes().length);

        // 验证磁盘文件是 zstd 压缩数据
        byte[] stored = FileUtil.readBytes(new File(filePath, docs.getName()));
        assertThat(StorageCompressor.isZstdCompressed(stored)).isTrue();

        // 解压后内容与原始一致
        byte[] decompressed = StorageCompressor.decompress(stored);
        assertThat(decompressed).isEqualTo(csvContent.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("上传已压缩格式 PDF → 不压缩存储 → DB 标记 compressed=0")
    void uploadPdfShouldNotCompress() throws Exception {
        byte[] pdfContent = new byte[4096];
        new java.util.Random().nextBytes(pdfContent);
        // PDF 魔数
        pdfContent[0] = 0x25;
        pdfContent[1] = 0x50;
        pdfContent[2] = 0x44;
        pdfContent[3] = 0x46;

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", pdfContent);

        ResponseDTO response = docsService.upload(file, 1);
        assertThat(response.getCode()).isEqualTo(200);

        Docs docs = (Docs) response.getData();
        assertThat(docs.getCompressed()).isEqualTo(0);
    }

    @Test
    @DisplayName("上传小文件 (<1KB) → 不压缩")
    void uploadSmallFileShouldNotCompress() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "small.csv", "text/csv", "a,b,c\n".getBytes(StandardCharsets.UTF_8));

        ResponseDTO response = docsService.upload(file, 1);
        assertThat(response.getCode()).isEqualTo(200);

        Docs docs = (Docs) response.getData();
        assertThat(docs.getCompressed()).isEqualTo(0);
    }

    @Test
    @DisplayName("重复上传相同文件 → MD5 去重 → 不新增物理文件")
    void duplicateUploadShouldDedup() throws Exception {
        String content = "col1,col2\n" + ("val1,val2\n").repeat(300);
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "data.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "data.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

        ResponseDTO r1 = docsService.upload(file1, 1);
        ResponseDTO r2 = docsService.upload(file2, 1);
        assertThat(r1.getCode()).isEqualTo(200);
        assertThat(r2.getCode()).isEqualTo(200);

        Docs d1 = (Docs) r1.getData();
        Docs d2 = (Docs) r2.getData();
        // 两次上传指向同一个物理文件
        assertThat(d1.getName()).isEqualTo(d2.getName());
    }
}
