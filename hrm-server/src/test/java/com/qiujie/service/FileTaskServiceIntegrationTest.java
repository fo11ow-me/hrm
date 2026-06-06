package com.qiujie.service;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.FileTask;
import com.qiujie.entity.FileTaskError;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.enums.TaskTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FileTaskService 集成测试。
 * 使用真实数据库验证任务生命周期、进度更新、错误查询等功能。
 *
 * @author qiujie
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class FileTaskServiceIntegrationTest {

    @Autowired
    private FileTaskService fileTaskService;

    @Autowired
    private FileTaskErrorService fileTaskErrorService;

    private Long createdTaskId;

    @BeforeEach
    void setUp() {
        createdTaskId = null;
    }

    // ==================== 任务生命周期 ====================

    @Test
    void taskLifecycle_ShouldTransitionCorrectly() {
        // 创建
        FileTask task = fileTaskService.createTask(TaskTypeEnum.IMPORT, TaskModuleEnum.ATTENDANCE,
                "test-lifecycle.xlsx", "/tmp/test.xlsx", null, 1);
        createdTaskId = task.getId();

        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.PENDING);
        assertThat(task.getId()).isNotNull();

        // 运行
        fileTaskService.markRunning(task.getId());
        FileTask running = fileTaskService.getById(task.getId());
        assertThat(running.getStatus()).isEqualTo(TaskStatusEnum.RUNNING);
        assertThat(running.getStartTime()).isNotNull();

        // 完成
        fileTaskService.finish(task.getId(), TaskStatusEnum.SUCCESS);
        FileTask finished = fileTaskService.getById(task.getId());
        assertThat(finished.getStatus()).isEqualTo(TaskStatusEnum.SUCCESS);
        assertThat(finished.getFinishTime()).isNotNull();
    }

    @Test
    void taskLifecycle_ShouldHandleFailure() {
        FileTask task = fileTaskService.createTask(TaskTypeEnum.EXPORT, TaskModuleEnum.ATTENDANCE,
                "test-fail.xlsx", null, null, 1);
        createdTaskId = task.getId();

        fileTaskService.markRunning(task.getId());
        fileTaskService.fail(task.getId(), "模拟错误");

        FileTask failed = fileTaskService.getById(task.getId());
        assertThat(failed.getStatus()).isEqualTo(TaskStatusEnum.FAILED);
        assertThat(failed.getFailReason()).isEqualTo("模拟错误");
    }

    // ==================== 进度更新 ====================

    @Test
    void increaseProgress_ShouldUpdateCounters() {
        FileTask task = fileTaskService.createTask(TaskTypeEnum.IMPORT, TaskModuleEnum.ATTENDANCE,
                "test-progress.xlsx", "/tmp/test.xlsx", null, 1);
        createdTaskId = task.getId();

        fileTaskService.increaseProgress(task.getId(), 100, 50, 45, 5);
        fileTaskService.increaseProgress(task.getId(), 0, 50, 48, 2);

        FileTask updated = fileTaskService.getById(task.getId());
        assertThat(updated.getTotalCount()).isEqualTo(100);
        assertThat(updated.getProcessedCount()).isEqualTo(100);
        assertThat(updated.getSuccessCount()).isEqualTo(93);
        assertThat(updated.getFailCount()).isEqualTo(7);
    }

    // ==================== 任务列表 ====================

    @Test
    void list_ShouldReturnPaginatedResults() {
        ResponseDTO result = fileTaskService.list(1, 10, null, null);
        assertThat(result.getCode()).isEqualTo(200);
    }

    // ==================== 错误查询 ====================

    @Test
    @Sql("/sql/init-file-task-test.sql")
    void queryErrors_ShouldReturnErrorList() {
        ResponseDTO result = fileTaskService.queryErrors(99993L, 1, 10);
        assertThat(result.getCode()).isEqualTo(200);
    }
}
