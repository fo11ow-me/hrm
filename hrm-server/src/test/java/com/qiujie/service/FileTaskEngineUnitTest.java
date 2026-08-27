package com.qiujie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.AttendanceImportRow;
import com.qiujie.entity.FileTask;
import com.qiujie.entity.FileTaskError;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.util.TestExcelUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileTaskEngine 单元测试。
 * 验证异步导入导出的核心逻辑：流式读取、分批处理、进度更新、错误处理、多页导出。
 *
 * @author qiujie
 */
@ExtendWith(MockitoExtension.class)
class FileTaskEngineUnitTest {

    @Mock
    private FileTaskService fileTaskService;

    @Mock
    private FileTaskErrorService fileTaskErrorService;

    @InjectMocks
    private FileTaskEngine fileTaskEngine;

    @TempDir
    Path tempDir;

    private File excelFile;

    @BeforeEach
    void setUp() {
        when(fileTaskService.claimRunning(anyLong())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (excelFile != null && excelFile.exists()) {
            excelFile.delete();
        }
    }

    // ==================== runImport 测试 ====================

    @Test
    void runImport_ValidRows_AllProcessed() throws IOException {
        excelFile = TestExcelUtil.createAttendanceImportExcel(
                tempDir.resolve("valid.xlsx").toString(), 300, "20240101");
        Long taskId = 1L;

        FileTask mockTask = new FileTask().setId(taskId).setSourceFilePath(excelFile.getAbsolutePath());
        when(fileTaskService.getById(taskId))
                .thenReturn(mockTask)
                .thenReturn(new FileTask().setId(taskId).setFailCount(0));

        ImportProcessor<AttendanceImportRow> processor = createMockProcessor(AttendanceImportRow.class, null);

        fileTaskEngine.runImport(taskId, processor);

        verify(fileTaskService).claimRunning(taskId);
        verify(processor, atLeastOnce()).processBatch(anyList(), eq(taskId), any());
        verify(fileTaskService).deleteSourceFile(taskId);
        verify(fileTaskService).finish(taskId, TaskStatusEnum.SUCCESS);
        verify(fileTaskService, never()).generateErrorFile(anyLong());
    }

    @Test
    void runImport_MultipleBatches_ShouldProcessAll() throws IOException {
        excelFile = TestExcelUtil.createAttendanceImportExcel(
                tempDir.resolve("large.xlsx").toString(), 1200, "20240101");
        Long taskId = 2L;

        FileTask mockTask = new FileTask().setId(taskId).setSourceFilePath(excelFile.getAbsolutePath());
        when(fileTaskService.getById(taskId))
                .thenReturn(mockTask)
                .thenReturn(new FileTask().setId(taskId).setFailCount(0));

        ImportProcessor<AttendanceImportRow> processor = createMockProcessor(AttendanceImportRow.class, null);

        fileTaskEngine.runImport(taskId, processor);

        // 1200 行 / 500 = 3 批
        verify(processor, times(3)).processBatch(anyList(), eq(taskId), any());
        verify(fileTaskService, times(3)).increaseProgress(anyLong(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(fileTaskService).deleteSourceFile(taskId);
        verify(fileTaskService).finish(taskId, TaskStatusEnum.SUCCESS);
    }

    @Test
    void runImport_WithErrors_PartialSuccess() throws IOException {
        excelFile = TestExcelUtil.createAttendanceImportExcel(
                tempDir.resolve("partial.xlsx").toString(), 300, "20240101");
        Long taskId = 3L;

        FileTask mockTask = new FileTask().setId(taskId).setSourceFilePath(excelFile.getAbsolutePath());
        when(fileTaskService.getById(taskId))
                .thenReturn(mockTask)
                .thenReturn(new FileTask().setId(taskId).setFailCount(5));

        // 模拟处理器报告错误
        ImportProcessor<AttendanceImportRow> processor = createMockProcessorWithErrors(AttendanceImportRow.class);

        fileTaskEngine.runImport(taskId, processor);

        verify(fileTaskService).finish(taskId, TaskStatusEnum.PARTIAL_SUCCESS);
        verify(fileTaskService).generateErrorFile(taskId);
        verify(fileTaskService, never()).deleteSourceFile(anyLong());
    }

    @Test
    void runImport_EmptyFile_ShouldFinishSuccess() throws IOException {
        excelFile = TestExcelUtil.createEmptyAttendanceImportExcel(
                tempDir.resolve("empty.xlsx").toString());
        Long taskId = 4L;

        FileTask mockTask = new FileTask().setId(taskId).setSourceFilePath(excelFile.getAbsolutePath());
        when(fileTaskService.getById(taskId))
                .thenReturn(mockTask)
                .thenReturn(new FileTask().setId(taskId).setFailCount(0));

        ImportProcessor<AttendanceImportRow> processor = createMockProcessor(AttendanceImportRow.class, null);

        fileTaskEngine.runImport(taskId, processor);

        verify(fileTaskService).deleteSourceFile(taskId);
        verify(fileTaskService).finish(taskId, TaskStatusEnum.SUCCESS);
        verify(processor, never()).processBatch(anyList(), anyLong(), any());
    }

    @Test
    void runImport_FileNotFound_ShouldCallFail() {
        Long taskId = 5L;
        FileTask mockTask = new FileTask().setId(taskId)
                .setSourceFilePath(tempDir.resolve("nonexistent.xlsx").toString());
        when(fileTaskService.getById(taskId)).thenReturn(mockTask);

        ImportProcessor<AttendanceImportRow> processor = createMockProcessor(AttendanceImportRow.class, null);

        fileTaskEngine.runImport(taskId, processor);

        verify(fileTaskService).fail(eq(taskId), any(Exception.class));
    }

    @Test
    void runImport_TaskNotFound_ShouldReturnSilently() {
        when(fileTaskService.getById(99L)).thenReturn(null);

        // lenient: task 为 null 时 processor 不会被调用
        @SuppressWarnings("unchecked")
        ImportProcessor<AttendanceImportRow> processor = mock(ImportProcessor.class);
        lenient().when(processor.getRowClass()).thenReturn(AttendanceImportRow.class);
        lenient().when(processor.headRowNumber()).thenReturn(2);

        fileTaskEngine.runImport(99L, processor);

        // markRunning 在 null 检查前调用，但后续流程不会继续
        verify(fileTaskService).claimRunning(99L);
        verify(processor, never()).processBatch(anyList(), anyLong(), any());
        verify(fileTaskService, never()).finish(anyLong(), any());
        verify(fileTaskService, never()).fail(anyLong(), (Exception) any());
    }

    @Test
    void runImport_ProcessorThrowsException_ShouldCallFail() throws IOException {
        excelFile = TestExcelUtil.createAttendanceImportExcel(
                tempDir.resolve("error.xlsx").toString(), 100, "20240101");
        Long taskId = 7L;

        FileTask mockTask = new FileTask().setId(taskId).setSourceFilePath(excelFile.getAbsolutePath());
        when(fileTaskService.getById(taskId)).thenReturn(mockTask);

        ImportProcessor<AttendanceImportRow> processor = mock(ImportProcessor.class);
        when(processor.getRowClass()).thenReturn(AttendanceImportRow.class);
        when(processor.headRowNumber()).thenReturn(2);
        doThrow(new RuntimeException("处理异常")).when(processor)
                .processBatch(anyList(), eq(taskId), any());

        fileTaskEngine.runImport(taskId, processor);

        verify(fileTaskService).fail(eq(taskId), any(RuntimeException.class));
    }

    // ==================== runExport 测试 ====================

    @Test
    void runExport_SinglePage_ShouldFinishSuccess() {
        Long taskId = 10L;

        File resultFile = new File(tempDir.toFile(), "task-result/test-uuid.xlsx");
        resultFile.getParentFile().mkdirs();
        when(fileTaskService.buildTaskFile("task-result", "test.xlsx")).thenReturn(resultFile);
        when(fileTaskService.uploadToMinio(any(File.class), anyString())).thenReturn("task-result/test-uuid.xlsx");

        List<AttendanceImportRow> data = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            AttendanceImportRow row = new AttendanceImportRow();
            row.setStaffId(i + 1);
            row.setAttendanceDate(new java.util.Date());
            data.add(row);
        }
        IPage<AttendanceImportRow> page = new Page<>(1, 500, 3);
        page.setRecords(data);

        @SuppressWarnings("unchecked")
        ExportProcessor<AttendanceImportRow> processor = mock(ExportProcessor.class);
        when(processor.getRowClass()).thenReturn(AttendanceImportRow.class);
        when(processor.queryPage(1, 500, "{}")).thenReturn(page);

        fileTaskEngine.runExport(taskId, processor, "{}", "test.xlsx");

        verify(fileTaskService).claimRunning(taskId);
        verify(fileTaskService).setTotalCount(taskId, 3);
        verify(fileTaskService).increaseProgress(taskId, 0, 3, 3, 0);
        verify(fileTaskService).setResultFile(eq(taskId), anyString());
        verify(fileTaskService).finish(taskId, TaskStatusEnum.SUCCESS);
    }

    @Test
    void runExport_MultiplePages_ShouldProcessAll() {
        Long taskId = 11L;

        File resultFile = new File(tempDir.toFile(), "task-result/test-multi.xlsx");
        resultFile.getParentFile().mkdirs();
        when(fileTaskService.buildTaskFile("task-result", "multi.xlsx")).thenReturn(resultFile);
        when(fileTaskService.uploadToMinio(any(File.class), anyString())).thenReturn("task-result/test-multi.xlsx");

        List<AttendanceImportRow> page1Data = new ArrayList<>();
        for (int i = 0; i < 500; i++) page1Data.add(new AttendanceImportRow());
        List<AttendanceImportRow> page2Data = new ArrayList<>();
        for (int i = 0; i < 500; i++) page2Data.add(new AttendanceImportRow());
        List<AttendanceImportRow> page3Data = new ArrayList<>();
        for (int i = 0; i < 200; i++) page3Data.add(new AttendanceImportRow());

        IPage<AttendanceImportRow> page1 = new Page<>(1, 500, 1200);
        page1.setRecords(page1Data);
        IPage<AttendanceImportRow> page2 = new Page<>(2, 500, 1200);
        page2.setRecords(page2Data);
        IPage<AttendanceImportRow> page3 = new Page<>(3, 500, 1200);
        page3.setRecords(page3Data);

        @SuppressWarnings("unchecked")
        ExportProcessor<AttendanceImportRow> processor = mock(ExportProcessor.class);
        when(processor.getRowClass()).thenReturn(AttendanceImportRow.class);
        when(processor.queryPage(1, 500, "{}")).thenReturn(page1);
        when(processor.queryPage(2, 500, "{}")).thenReturn(page2);
        when(processor.queryPage(3, 500, "{}")).thenReturn(page3);

        fileTaskEngine.runExport(taskId, processor, "{}", "multi.xlsx");

        verify(processor, times(3)).queryPage(anyInt(), eq(500), eq("{}"));
        verify(fileTaskService, times(3)).increaseProgress(anyLong(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(fileTaskService).finish(taskId, TaskStatusEnum.SUCCESS);
    }

    @Test
    void runExport_EmptyResult_ShouldFinishSuccess() {
        Long taskId = 12L;

        File resultFile = new File(tempDir.toFile(), "task-result/test-empty.xlsx");
        resultFile.getParentFile().mkdirs();
        when(fileTaskService.buildTaskFile("task-result", "empty.xlsx")).thenReturn(resultFile);
        when(fileTaskService.uploadToMinio(any(File.class), anyString())).thenReturn("task-result/test-empty.xlsx");

        IPage<AttendanceImportRow> page = new Page<>(1, 500, 0);
        page.setRecords(new ArrayList<>());

        @SuppressWarnings("unchecked")
        ExportProcessor<AttendanceImportRow> processor = mock(ExportProcessor.class);
        when(processor.getRowClass()).thenReturn(AttendanceImportRow.class);
        when(processor.queryPage(1, 500, "{}")).thenReturn(page);

        fileTaskEngine.runExport(taskId, processor, "{}", "empty.xlsx");

        verify(fileTaskService).claimRunning(taskId);
        verify(fileTaskService, never()).setTotalCount(anyLong(), anyInt());
        verify(fileTaskService).finish(taskId, TaskStatusEnum.SUCCESS);
    }

    @Test
    void runExport_ProcessorThrowsException_ShouldCallFail() {
        Long taskId = 13L;

        File resultFile = new File(tempDir.toFile(), "task-result/test-fail.xlsx");
        resultFile.getParentFile().mkdirs();
        when(fileTaskService.buildTaskFile("task-result", "fail.xlsx")).thenReturn(resultFile);

        @SuppressWarnings("unchecked")
        ExportProcessor<AttendanceImportRow> processor = mock(ExportProcessor.class);
        when(processor.getRowClass()).thenReturn(AttendanceImportRow.class);
        when(processor.queryPage(1, 500, "{}")).thenThrow(new RuntimeException("查询异常"));

        fileTaskEngine.runExport(taskId, processor, "{}", "fail.xlsx");

        verify(fileTaskService).fail(eq(taskId), any(RuntimeException.class));
    }

    // ==================== helper ====================

    @SuppressWarnings("unchecked")
    private <T> ImportProcessor<T> createMockProcessor(Class<T> rowClass, TaskModuleEnum module) {
        ImportProcessor<T> processor = mock(ImportProcessor.class);
        when(processor.getRowClass()).thenReturn(rowClass);
        when(processor.headRowNumber()).thenReturn(2);
        if (module != null) {
            when(processor.getModule()).thenReturn(module);
        }
        return processor;
    }

    @SuppressWarnings("unchecked")
    private <T> ImportProcessor<T> createMockProcessorWithErrors(Class<T> rowClass) {
        ImportProcessor<T> processor = mock(ImportProcessor.class);
        when(processor.getRowClass()).thenReturn(rowClass);
        when(processor.headRowNumber()).thenReturn(2);
        // 模拟 processBatch：调用 errorCollector 报告错误
        doAnswer(invocation -> {
            Consumer<FileTaskError> errorCollector = invocation.getArgument(2);
            errorCollector.accept(new FileTaskError()
                    .setRowNum(3)
                    .setRawData("test")
                    .setErrorMessage("测试错误"));
            return null;
        }).when(processor).processBatch(anyList(), anyLong(), any());
        return processor;
    }
}
