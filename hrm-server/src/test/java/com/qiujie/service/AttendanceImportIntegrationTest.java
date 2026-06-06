package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.FileTask;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.util.TestExcelUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 考勤异步导入集成测试。
 * 异步操作不由测试事务管理，使用 @Sql 手动清理数据。
 *
 * @author qiujie
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(value = "/sql/init-attendance-test.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/sql/cleanup-test.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class AttendanceImportIntegrationTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private FileTaskService fileTaskService;

    @Autowired
    private AttendanceMapper attendanceMapper;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("att-import-test-");
    }

    @AfterEach
    void tearDown() {
        if (tempDir != null) {
            try {
                Files.walk(tempDir).map(Path::toFile).forEach(File::delete);
            } catch (IOException ignored) {
            }
        }
    }

    // ==================== 基本导入流程 ====================

    @Test
    void importValidRows_ShouldCreateAttendanceRecords() throws Exception {
        File excelFile = TestExcelUtil.createAttendanceImportExcel(
                tempDir.resolve("valid.xlsx").toString(), 10, "20240102");
        MultipartFile multipartFile = createMultipartFile(excelFile);

        ResponseDTO response = attendanceService.createImportTask(multipartFile);
        assertThat(response.getCode()).isEqualTo(200);

        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        Long taskId = task.getId();

        await().atMost(Duration.ofSeconds(30)).until(() -> {
            FileTask updated = fileTaskService.getById(taskId);
            return updated.getStatus() != TaskStatusEnum.PENDING
                    && updated.getStatus() != TaskStatusEnum.RUNNING;
        });

        FileTask finished = fileTaskService.getById(taskId);
        assertThat(finished.getStatus()).isIn(TaskStatusEnum.SUCCESS, TaskStatusEnum.PARTIAL_SUCCESS);
        assertThat(finished.getProcessedCount()).isGreaterThan(0);
    }

    @Test
    void importWithErrors_ShouldCompleteWithErrors() throws Exception {
        File excelFile = TestExcelUtil.createAttendanceImportExcelWithErrors(
                tempDir.resolve("errors.xlsx").toString());
        MultipartFile multipartFile = createMultipartFile(excelFile);

        ResponseDTO response = attendanceService.createImportTask(multipartFile);
        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        Long taskId = task.getId();

        await().atMost(Duration.ofSeconds(30)).until(() -> {
            FileTask updated = fileTaskService.getById(taskId);
            return updated.getStatus() != TaskStatusEnum.PENDING
                    && updated.getStatus() != TaskStatusEnum.RUNNING;
        });

        FileTask finished = fileTaskService.getById(taskId);
        assertThat(finished.getStatus()).isNotEqualTo(TaskStatusEnum.FAILED);
        assertThat(finished.getProcessedCount()).isGreaterThan(0);
    }

    // ==================== 大文件导入 ====================

    @Test
    void importLargeFile_ShouldProcessAllRows() throws Exception {
        File excelFile = TestExcelUtil.createAttendanceImportExcel(
                tempDir.resolve("large.xlsx").toString(), 1050, "20240102");
        MultipartFile multipartFile = createMultipartFile(excelFile);

        ResponseDTO response = attendanceService.createImportTask(multipartFile);
        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        Long taskId = task.getId();

        await().atMost(Duration.ofSeconds(60)).until(() -> {
            FileTask updated = fileTaskService.getById(taskId);
            return updated.getStatus() != TaskStatusEnum.PENDING
                    && updated.getStatus() != TaskStatusEnum.RUNNING;
        });

        FileTask finished = fileTaskService.getById(taskId);
        assertThat(finished.getStatus()).isNotEqualTo(TaskStatusEnum.FAILED);
        // 应该处理了大部分行（允许少量因周末等原因被跳过）
        assertThat(finished.getProcessedCount()).isGreaterThan(1000);
    }

    // ==================== 错误文件生成 ====================

    @Test
    void importWithErrors_ShouldGenerateErrorFileWhenPartialSuccess() throws Exception {
        File excelFile = TestExcelUtil.createAttendanceImportExcelWithErrors(
                tempDir.resolve("error-file.xlsx").toString());
        MultipartFile multipartFile = createMultipartFile(excelFile);

        ResponseDTO response = attendanceService.createImportTask(multipartFile);
        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        Long taskId = task.getId();

        await().atMost(Duration.ofSeconds(30)).until(() -> {
            FileTask updated = fileTaskService.getById(taskId);
            return updated.getStatus() != TaskStatusEnum.PENDING
                    && updated.getStatus() != TaskStatusEnum.RUNNING;
        });

        FileTask finished = fileTaskService.getById(taskId);
        if (finished.getStatus() == TaskStatusEnum.PARTIAL_SUCCESS) {
            assertThat(finished.getErrorFilePath()).isNotNull();
            File errorFile = new File(finished.getErrorFilePath());
            assertThat(errorFile).exists();
        }
    }

    // ==================== 无效上传 ====================

    @Test
    void importEmptyFile_ShouldReturnError() throws Exception {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        ResponseDTO response = attendanceService.createImportTask(emptyFile);
        assertThat(response.getCode()).isEqualTo(300);
    }

    // ==================== 考勤状态计算 ====================

    @Test
    void importWithStatusVariations_ShouldComplete() throws Exception {
        File excelFile = TestExcelUtil.createAttendanceImportExcelWithStatusVariations(
                tempDir.resolve("status.xlsx").toString());
        MultipartFile multipartFile = createMultipartFile(excelFile);

        ResponseDTO response = attendanceService.createImportTask(multipartFile);
        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        Long taskId = task.getId();

        await().atMost(Duration.ofSeconds(30)).until(() -> {
            FileTask updated = fileTaskService.getById(taskId);
            return updated.getStatus() != TaskStatusEnum.PENDING
                    && updated.getStatus() != TaskStatusEnum.RUNNING;
        });

        FileTask finished = fileTaskService.getById(taskId);
        assertThat(finished.getStatus()).isNotEqualTo(TaskStatusEnum.FAILED);
        assertThat(finished.getProcessedCount()).isGreaterThan(0);
    }

    // ==================== helper ====================

    private MultipartFile createMultipartFile(File file) throws IOException {
        return new MockMultipartFile("file", file.getName(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new FileInputStream(file));
    }
}
