package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.IngestionJob;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.DeleteCommand;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.DeleteResult;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RegisterCommand;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RegisterResult;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RetryCommand;
import com.qiujie.knowledge.lifecycle.port.EmbeddingProvider;
import com.qiujie.knowledge.lifecycle.support.FixedEmbeddingProvider;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.qiujie.knowledge.service.HybridRetrievalService;
import com.qiujie.knowledge.spi.KnowledgeSearchProvider.SearchResult;
import com.qiujie.storage.MinioStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文档生命周期集成测试：Testcontainers 真实 MySQL + pgvector + MinIO。
 * 验证 CAS SQL 语义、真实 ETL 落库、镜像表 upsert 后关键词检索可用、删除级联。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("文档生命周期集成测试")
class DocumentLifecycleIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.1")
            .withDatabaseName("hrm")
            .withInitScript("sql/lifecycle-it-mysql.sql");
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("hrm_kb")
            .withInitScript("sql/lifecycle-it-pg.sql");
    static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("minio/minio:latest"));

    static {
        MYSQL.start();
        POSTGRES.start();
        MINIO.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.master.jdbc-url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.master.username", MYSQL::getUsername);
        registry.add("spring.datasource.master.password", MYSQL::getPassword);
        registry.add("spring.datasource.flowable.jdbc-url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.flowable.username", MYSQL::getUsername);
        registry.add("spring.datasource.flowable.password", MYSQL::getPassword);
        registry.add("spring.datasource.kb.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.kb.username", POSTGRES::getUsername);
        registry.add("spring.datasource.kb.password", POSTGRES::getPassword);
        registry.add("storage.minio.endpoint", MINIO::getS3URL);
        registry.add("storage.minio.access-key", MINIO::getUserName);
        registry.add("storage.minio.secret-key", MINIO::getPassword);
        registry.add("knowledge.enabled", () -> "true");
        // JwtUtil 将 secret 按 base64 解码且要求 ≥256 位
        registry.add("TEST_JWT_SECRET", () -> "aW50ZWdyYXRpb24tdGVzdC1zZWNyZXQtcGFkZGluZy0wMTIzNDU2Nzg5");
        // DashScope 自动装配要求 api-key 非空（测试不触达外部 API）
        registry.add("DASHSCOPE_API_KEY", () -> "integration-test-dummy-key");
    }

    /** 嵌入提供商替换为固定 1024 维向量（pgvector 列维度要求），不触达外部 DashScope。 */
    @TestConfiguration
    static class EmbeddingTestConfig {
        @Bean
        @Primary
        EmbeddingProvider fixedEmbeddingProvider() {
            return new FixedEmbeddingProvider(1024);
        }
    }

    @Autowired
    private DocumentLifecycleService lifecycle;

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private MinioStorageService minioStorage;

    @Autowired
    private HybridRetrievalService retrieval;

    @Autowired
    @Qualifier("kbDataSource")
    private DataSource kbDataSource;

    @Test
    @DisplayName("register → 真实 ETL → READY：切片/向量/镜像全落库，元数据契约修复")
    void register_ShouldIngestToReady_WithRealDatabases() throws Exception {
        String key = "knowledge/9/it/手册.txt";
        minioStorage.put(key, "第一段集成内容\n\n第二段集成内容".getBytes(StandardCharsets.UTF_8));

        RegisterResult result = lifecycle.register(new RegisterCommand(
                key, "集成测试手册.txt", "txt", "hash-it-1", 200L, 9));

        KnowledgeDocument doc = awaitStatus(result.documentId(), "READY", 60_000);

        // MySQL：文档结算 READY，失败原因清空，切片数落库
        assertEquals("READY", doc.getStatus());
        assertEquals(1, doc.getChunkCount());
        assertNull(doc.getFailureReason());
        // MySQL：作业 SUCCEEDED
        JdbcTemplate kb = new JdbcTemplate(kbDataSource);
        // PG：切片 1 条、向量 1 条
        assertEquals(1, count(kb, "SELECT COUNT(*) FROM document_chunk WHERE document_id = ?",
                result.documentId()));
        assertEquals(1, count(kb, "SELECT COUNT(*) FROM vector_store "
                + "WHERE metadata::jsonb ->> 'documentId' = ?", String.valueOf(result.documentId())));
        // 契约修复：向量元数据携带文档名
        String meta = kb.queryForObject(
                "SELECT metadata::text FROM vector_store "
                + "WHERE metadata::jsonb ->> 'documentId' = ? LIMIT 1",
                String.class, String.valueOf(result.documentId()));
        assertEquals("集成测试手册.txt", VectorMetadata.fromJson(meta).documentName());
        // PG 镜像行已同步（关键词检索 JOIN 来源）
        String mirrorName = kb.queryForObject(
                "SELECT old_name FROM kb_document WHERE id = ?", String.class, result.documentId());
        assertEquals("集成测试手册.txt", mirrorName);
    }

    @Test
    @DisplayName("镜像表写入后关键词检索应返回结果（修复恒空 bug）")
    void keywordSearch_ShouldReturnResults_AfterMirrorUpsert() throws Exception {
        String key = "knowledge/9/it/检索.txt";
        minioStorage.put(key, "关键词检索专用内容段落\n\n第二段".getBytes(StandardCharsets.UTF_8));
        RegisterResult result = lifecycle.register(new RegisterCommand(
                key, "检索手册.txt", "txt", "hash-it-2", 200L, 9));
        awaitStatus(result.documentId(), "READY", 60_000);

        // HybridRetrievalService.search：关键词通道 JOIN PG 镜像表
        List<SearchResult> results = retrieval.search(List.of("关键词检索专用"));

        assertFalse(results.isEmpty(), "关键词检索不应为空（镜像表未被写入时恒空）");
        assertTrue(results.stream().anyMatch(r -> "检索手册.txt".equals(r.documentName())),
                "检索结果应携带文档名");
    }

    @Test
    @DisplayName("删除级联：MySQL 逻辑删 + MinIO 物理文件 + PG 切片/向量/镜像全清理")
    void delete_ShouldCascadeCleanup_WithRealDatabases() throws Exception {
        String key = "knowledge/9/it/删除.txt";
        minioStorage.put(key, "待删除内容".getBytes(StandardCharsets.UTF_8));
        RegisterResult result = lifecycle.register(new RegisterCommand(
                key, "删除手册.txt", "txt", "hash-it-3", 200L, 9));
        Long docId = result.documentId();
        awaitStatus(docId, "READY", 60_000);

        DeleteResult deleteResult = lifecycle.delete(new DeleteCommand(docId));

        assertFalse(deleteResult.alreadyDeleted());
        // MySQL 逻辑删
        assertEquals(1, documentMapper.selectById(docId).getIsDeleted());
        // MinIO 物理文件已删
        assertFalse(minioStorage.exists(key));
        // PG 产物全清理（幂等 purge 在提交后异步执行，轮询等待）
        JdbcTemplate kb = new JdbcTemplate(kbDataSource);
        awaitCondition(() ->
                count(kb, "SELECT COUNT(*) FROM document_chunk WHERE document_id = ?", docId) == 0
                && count(kb, "SELECT COUNT(*) FROM vector_store "
                        + "WHERE metadata::jsonb ->> 'documentId' = ?", String.valueOf(docId)) == 0
                && count(kb, "SELECT COUNT(*) FROM kb_document WHERE id = ?", docId) == 0,
                30_000);
        // 已删文档重试被拒绝
        assertFalse(lifecycle.retry(new RetryCommand(docId)).accepted());
    }

    @Test
    @DisplayName("CAS SQL 语义：认领互斥、READY 拒绝、已删不复活、存活引用计数")
    void casSemantics_ShouldHold_WithRealDatabase() {
        // UPLOADED → 认领成功，再次认领失败
        KnowledgeDocument doc = insertDoc("knowledge/9/it/cas1.txt", "cas1.txt", "UPLOADED");
        assertEquals(1, documentMapper.claimForProcessing(doc.getId()));
        assertEquals("PROCESSING", documentMapper.selectById(doc.getId()).getStatus());
        assertEquals(0, documentMapper.claimForProcessing(doc.getId()));
        // PROCESSING → READY 结算
        assertEquals(1, documentMapper.completeProcessing(doc.getId(), "预览", 3));
        KnowledgeDocument ready = documentMapper.selectById(doc.getId());
        assertEquals("READY", ready.getStatus());
        assertEquals("预览", ready.getPreviewText());
        assertNull(ready.getFailureReason());
        // READY 拒绝再认领
        assertEquals(0, documentMapper.claimForProcessing(doc.getId()));

        // 已删文档：结算失败（不复活）、markDeleted 幂等
        KnowledgeDocument deleted = insertDoc("knowledge/9/it/cas2.txt", "cas2.txt", "PROCESSING");
        assertEquals(1, documentMapper.markDeleted(deleted.getId()));
        assertEquals(0, documentMapper.markDeleted(deleted.getId()));
        assertEquals(0, documentMapper.completeProcessing(deleted.getId(), "预览", 1));

        // 存活引用计数：同名文件一存活一已删 → 只计存活
        KnowledgeDocument live = insertDoc("knowledge/9/it/shared.txt", "shared.txt", "UPLOADED");
        assertEquals(1L, documentMapper.countLiveByFileName("knowledge/9/it/shared.txt", 9999L));
    }

    // ==================== 辅助 ====================

    private KnowledgeDocument insertDoc(String name, String oldName, String status) {
        KnowledgeDocument doc = new KnowledgeDocument()
                .setName(name)
                .setOldName(oldName)
                .setType("txt")
                .setFileHash("hash-" + name)
                .setFileSize(100L)
                .setStatus(status)
                .setStaffId(9);
        documentMapper.insert(doc);
        return doc;
    }

    private KnowledgeDocument awaitStatus(Long documentId, String status, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        KnowledgeDocument doc = null;
        while (System.currentTimeMillis() < deadline) {
            doc = documentMapper.selectById(documentId);
            if (doc != null && status.equals(doc.getStatus())) {
                return doc;
            }
            Thread.sleep(300);
        }
        fail("等待状态超时: expected=" + status + " actual=" + (doc != null ? doc.getStatus() : "null"));
        return null;
    }

    private void awaitCondition(java.util.function.BooleanSupplier condition, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(300);
        }
        fail("等待条件超时");
    }

    private int count(JdbcTemplate jdbc, String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
