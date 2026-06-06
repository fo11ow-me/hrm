package com.qiujie.service;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.FileTask;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.mapper.AttendanceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

import java.io.File;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 考勤异步导出集成测试。
 * 异步操作不由测试事务管理，需手动清理。
 *
 * @author qiujie
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(value = "/sql/init-attendance-test.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/sql/cleanup-test.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class AttendanceExportIntegrationTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private FileTaskService fileTaskService;

    @Autowired
    private AttendanceMapper attendanceMapper;

    // ==================== 基本导出流程 ====================

    @Test
    void export_ShouldCreateResultFile() {
        ResponseDTO response = attendanceService.createExportTask("202401", "test-export.xlsx");
        assertThat(response.getCode()).isEqualTo(200);

        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        Long taskId = task.getId();

        await().atMost(Duration.ofSeconds(30)).until(() -> {
            FileTask updated = fileTaskService.getById(taskId);
            return updated.getStatus() == TaskStatusEnum.SUCCESS
                    || updated.getStatus() == TaskStatusEnum.FAILED;
        });

        FileTask finished = fileTaskService.getById(taskId);
        assertThat(finished.getStatus()).isEqualTo(TaskStatusEnum.SUCCESS);
        assertThat(finished.getResultFilePath()).isNotNull();

        File resultFile = new File(finished.getResultFilePath());
        assertThat(resultFile).exists();
    }

    // ==================== 空数据导出 ====================

    @Test
    void export_NoDataMonth_ShouldStillSucceed() {
        ResponseDTO response = attendanceService.createExportTask("202312", "empty-month.xlsx");
        assertThat(response.getCode()).isEqualTo(200);

        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        Long taskId = task.getId();

        await().atMost(Duration.ofSeconds(30)).until(() -> {
            FileTask updated = fileTaskService.getById(taskId);
            return updated.getStatus() == TaskStatusEnum.SUCCESS
                    || updated.getStatus() == TaskStatusEnum.FAILED;
        });

        FileTask finished = fileTaskService.getById(taskId);
        assertThat(finished.getStatus()).isEqualTo(TaskStatusEnum.SUCCESS);
    }

    // ==================== 默认参数导出 ====================

    @Test
    void export_WithDefaultParams_ShouldUseCurrentMonth() {
        ResponseDTO response = attendanceService.createExportTask(null, null);
        assertThat(response.getCode()).isEqualTo(200);

        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        assertThat(task.getFileName()).contains("attendance_report");
    }

    // ==================== 含考勤数据的导出 ====================

    @Test
    void export_WithAttendanceData_ShouldIncludeSummary() {
        // 先插入一些考勤数据
        Attendance att = new Attendance();
        att.setStaffId(1);
        att.setAttendanceDate(Date.valueOf(LocalDate.of(2024, 1, 2)));
        att.setStatus(AttendanceStatusEnum.NORMAL);
        attendanceMapper.insert(att);

        ResponseDTO response = attendanceService.createExportTask("202401", "summary-test.xlsx");
        assertThat(response.getCode()).isEqualTo(200);

        FileTask task = (FileTask) response.getData();
        assertThat(task).isNotNull();
        Long taskId = task.getId();

        await().atMost(Duration.ofSeconds(30)).until(() -> {
            FileTask updated = fileTaskService.getById(taskId);
            return updated.getStatus() == TaskStatusEnum.SUCCESS
                    || updated.getStatus() == TaskStatusEnum.FAILED;
        });

        FileTask finished = fileTaskService.getById(taskId);
        assertThat(finished.getStatus()).isEqualTo(TaskStatusEnum.SUCCESS);
        assertThat(finished.getResultFilePath()).isNotNull();
    }
}
