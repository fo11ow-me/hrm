package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.FileTask;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.enums.TaskTypeEnum;
import com.qiujie.mapper.FileTaskMapper;
import com.qiujie.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileTaskService 单元测试。
 * 验证任务生命周期管理、进度更新、错误文件生成、过期清理等逻辑。
 *
 * @author qiujie
 */
@ExtendWith(MockitoExtension.class)
class FileTaskServiceUnitTest {

    @Mock
    private FileTaskMapper fileTaskMapper;

    @Mock
    private FileTaskErrorService fileTaskErrorService;

    @Mock
    private FileTaskSseService sseService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private FileTaskService fileTaskService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileTaskService, "filePath", tempDir.toString());
    }

    // ==================== createTask ====================

    @Test
    void createTask_ShouldCreatePendingTask() {
        FileTask task = fileTaskService.createTask(TaskTypeEnum.IMPORT, TaskModuleEnum.ATTENDANCE,
                "test.xlsx", "/tmp/source.xlsx", null, 1);

        assertThat(task.getTaskType()).isEqualTo(TaskTypeEnum.IMPORT);
        assertThat(task.getModule()).isEqualTo(TaskModuleEnum.ATTENDANCE);
        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.PENDING);
        assertThat(task.getFileName()).isEqualTo("test.xlsx");
        assertThat(task.getTotalCount()).isZero();
        assertThat(task.getProcessedCount()).isZero();
        assertThat(task.getSuccessCount()).isZero();
        assertThat(task.getFailCount()).isZero();
    }

    // ==================== finish ====================

    @Test
    void finish_ShouldSetStatusAndFinishTime() {
        fileTaskService.finish(1L, TaskStatusEnum.SUCCESS);

        ArgumentCaptor<FileTask> captor = ArgumentCaptor.forClass(FileTask.class);
        verify(fileTaskMapper).updateById(captor.capture());
        FileTask updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getStatus()).isEqualTo(TaskStatusEnum.SUCCESS);
        assertThat(updated.getFinishTime()).isNotNull();
    }

    // ==================== fail ====================

    @Test
    void fail_ShouldSetFailedStatusAndReason() {
        fileTaskService.fail(1L, new RuntimeException("处理异常"));

        ArgumentCaptor<FileTask> captor = ArgumentCaptor.forClass(FileTask.class);
        verify(fileTaskMapper).updateById(captor.capture());
        FileTask updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(TaskStatusEnum.FAILED);
        assertThat(updated.getFailReason()).isEqualTo("处理异常");
        assertThat(updated.getFinishTime()).isNotNull();
    }

    @Test
    void fail_LongMessage_ShouldTruncateTo1000() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append("x");
        }
        String longMsg = sb.toString();

        fileTaskService.fail(1L, longMsg);

        ArgumentCaptor<FileTask> captor = ArgumentCaptor.forClass(FileTask.class);
        verify(fileTaskMapper).updateById(captor.capture());
        assertThat(captor.getValue().getFailReason()).hasSize(1000);
    }

    // ==================== increaseProgress ====================

    @Test
    void increaseProgress_ShouldCallMapperAndPushSse() {
        when(fileTaskMapper.increaseProgress(eq(1L), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(1);
        FileTask task = new FileTask().setId(1L).setOperatorId(1);
        when(fileTaskMapper.selectById(1L)).thenReturn(task);

        fileTaskService.increaseProgress(1L, 100, 50, 45, 5);

        verify(fileTaskMapper).increaseProgress(1L, 100, 50, 45, 5);
        verify(sseService).emit(any(FileTask.class));
    }

    // ==================== setTotalCount ====================

    @Test
    void setTotalCount_ShouldUpdateTask() {
        fileTaskService.setTotalCount(1L, 500);

        verify(fileTaskMapper).updateById(argThat(t -> t.getId().equals(1L) && t.getTotalCount() == 500));
    }

    // ==================== setResultFile ====================

    @Test
    void setResultFile_ShouldUpdateTask() {
        fileTaskService.setResultFile(1L, "/tmp/result.xlsx");

        verify(fileTaskMapper).updateById(argThat(t ->
                t.getId().equals(1L) && "/tmp/result.xlsx".equals(t.getResultFilePath())));
    }

    // ==================== buildTaskFile ====================

    @Test
    void buildTaskFile_ShouldCreateFileInCorrectDirectory() {
        File file = fileTaskService.buildTaskFile("task-source", "test.xlsx");
        assertThat(file).isNotNull();
        assertThat(file.getParentFile()).exists();
        assertThat(file.getName()).endsWith(".xlsx");
        assertThat(file.getName()).doesNotContain("test"); // UUID 文件名，不含原始名
    }

    // ==================== cleanExpiredTaskFiles ====================

    @Test
    void cleanExpiredTaskFiles_ShouldDeleteExpiredTasks() {
        // 使用 spy 来 mock ServiceImpl.removeById（避免 MyBatis-Plus tableInfo NPE）
        FileTaskService spy = spy(fileTaskService);
        doReturn(true).when(spy).removeById(anyLong());

        FileTask expired1 = new FileTask().setId(1L).setSourceFilePath("/tmp/old1.xlsx");
        FileTask expired2 = new FileTask().setId(2L).setSourceFilePath("/tmp/old2.xlsx")
                .setResultFilePath("/tmp/result2.xlsx").setErrorFilePath("/tmp/error2.xlsx");
        when(fileTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(expired1, expired2));
        when(fileTaskErrorService.remove(any(QueryWrapper.class))).thenReturn(true);

        spy.cleanExpiredTaskFiles();

        verify(fileTaskErrorService, times(2)).remove(any(QueryWrapper.class));
        verify(spy, times(2)).removeById(anyLong());
    }

    // ==================== list ====================

    @Test
    void list_ShouldFilterByTaskTypeAndModule() {
        when(securityUtil.getCurrentOperatorId()).thenReturn(1);
        Page<FileTask> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        page.setTotal(0);
        when(fileTaskMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

        ResponseDTO result = fileTaskService.list(1, 10, "IMPORT", "ATTENDANCE");
        assertThat(result.getCode()).isEqualTo(200);
    }

    // ==================== query ====================

    @Test
    void query_TaskNotFound_ShouldReturnError() {
        when(fileTaskMapper.selectById(999L)).thenReturn(null);

        ResponseDTO result = fileTaskService.query(999L);
        assertThat(result.getCode()).isEqualTo(300);
    }

    @Test
    void query_TaskBelongsToOtherUser_ShouldReturnError() {
        when(securityUtil.getCurrentOperatorId()).thenReturn(1);
        FileTask task = new FileTask().setId(1L).setOperatorId(2);
        when(fileTaskMapper.selectById(1L)).thenReturn(task);

        ResponseDTO result = fileTaskService.query(1L);
        assertThat(result.getCode()).isEqualTo(300);
    }

    // ==================== queryErrors ====================

    @Test
    void queryErrors_TaskNotFound_ShouldReturnError() {
        when(fileTaskMapper.selectById(999L)).thenReturn(null);

        ResponseDTO result = fileTaskService.queryErrors(999L, 1, 10);
        assertThat(result.getCode()).isEqualTo(300);
    }
}
