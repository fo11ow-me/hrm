package com.qiujie.service;

import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.FileTask;
import com.qiujie.enums.TaskModuleEnum;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileTaskCoordinatorUnitTest {

    @Mock
    private FileTaskService fileTaskService;

    @Mock
    private FileTaskEngine fileTaskEngine;

    @Mock
    private ThreadPoolTaskExecutor executor;

    @Test
    void facadeDelegatesQueryOperations() throws Exception {
        FileTaskService service = mock(FileTaskService.class);
        FileTaskEngine engine = mock(FileTaskEngine.class);
        ThreadPoolTaskExecutor taskExecutor = mock(ThreadPoolTaskExecutor.class);
        FileTaskCoordinator coordinator = new FileTaskCoordinator(service, engine, taskExecutor);
        ResponseDTO expected = Response.success();
        SseEmitter emitter = new SseEmitter();

        when(service.list(1, 10, "IMPORT", "ATTENDANCE")).thenReturn(expected);
        when(service.query(7L)).thenReturn(expected);
        when(service.queryErrors(7L, 1, 10)).thenReturn(expected);
        when(service.subscribeSse()).thenReturn(emitter);

        assertThat(coordinator.list(1, 10, "IMPORT", "ATTENDANCE")).isSameAs(expected);
        assertThat(coordinator.inspect(7L)).isSameAs(expected);
        assertThat(coordinator.queryErrors(7L, 1, 10)).isSameAs(expected);
        assertThat(coordinator.subscribe()).isSameAs(emitter);
        verify(service).list(1, 10, "IMPORT", "ATTENDANCE");
        verify(service).query(7L);
        verify(service).queryErrors(7L, 1, 10);
        verify(service).subscribeSse();
    }

    @Test
    void facadeDelegatesDownload() throws Exception {
        FileTaskService service = mock(FileTaskService.class);
        FileTaskCoordinator coordinator = new FileTaskCoordinator(
                service, mock(FileTaskEngine.class), mock(ThreadPoolTaskExecutor.class));
        jakarta.servlet.http.HttpServletResponse response = mock(jakarta.servlet.http.HttpServletResponse.class);

        coordinator.download(7L, "RESULT", response);

        verify(service).download(7L, "RESULT", response);
    }

    @Test
    void submitImport_createsTaskAndSchedulesDefaultReader() {
        FileTask task = new FileTask().setId(7L);
        when(fileTaskService.createTask(any(), eq(TaskModuleEnum.ATTENDANCE), anyString(), anyString(), isNull(), eq(3)))
                .thenReturn(task);
        AtomicReference<Runnable> submitted = new AtomicReference<>();
        doAnswer(invocation -> {
            submitted.set(invocation.getArgument(0));
            return null;
        }).when(executor).execute(any(Runnable.class));

        FileTaskCoordinator coordinator = new FileTaskCoordinator(fileTaskService, fileTaskEngine, executor);
        ImportProcessor<Object> processor = mock(ImportProcessor.class);
        FileTaskCoordinator.TaskSubmission result = coordinator.submitImport(
                new FileTaskCoordinator.ImportCommand(
                        TaskModuleEnum.ATTENDANCE, "attendance.xlsx", "source-key", null, 3),
                processor);

        assertThat(result.taskId()).isEqualTo(7L);
        assertThat(result.snapshot()).isSameAs(task);
        verify(fileTaskService).createTask(any(), eq(TaskModuleEnum.ATTENDANCE), eq("attendance.xlsx"),
                eq("source-key"), isNull(), eq(3));
        assertThat(submitted.get()).isNotNull();
        submitted.get().run();
        verify(fileTaskEngine).runImport(eq(7L), same(processor));
    }

    @Test
    void submitImport_preservesCustomReader() {
        FileTask task = new FileTask().setId(8L);
        when(fileTaskService.createTask(any(), any(), anyString(), any(), any(), anyInt())).thenReturn(task);
        AtomicReference<Runnable> submitted = new AtomicReference<>();
        doAnswer(invocation -> {
            submitted.set(invocation.getArgument(0));
            return null;
        }).when(executor).execute(any(Runnable.class));

        FileTaskCoordinator coordinator = new FileTaskCoordinator(fileTaskService, fileTaskEngine, executor);
        ImportProcessor<Object> processor = mock(ImportProcessor.class);
        ImportReader<Object> reader = mock(ImportReader.class);
        coordinator.submitImport(
                new FileTaskCoordinator.ImportCommand(TaskModuleEnum.STAFF_OVERTIME,
                        "overtime.xlsx", "key", "{}", 4), processor, reader);

        submitted.get().run();
        verify(fileTaskEngine).runImport(8L, processor, reader);
    }

    @Test
    void submitExport_createsTaskAndSchedulesEngine() {
        FileTask task = new FileTask().setId(9L);
        when(fileTaskService.createTask(any(), eq(TaskModuleEnum.SALARY), anyString(), isNull(), eq("202608"), eq(5)))
                .thenReturn(task);
        AtomicReference<Runnable> submitted = new AtomicReference<>();
        doAnswer(invocation -> {
            submitted.set(invocation.getArgument(0));
            return null;
        }).when(executor).execute(any(Runnable.class));

        FileTaskCoordinator coordinator = new FileTaskCoordinator(fileTaskService, fileTaskEngine, executor);
        ExportProcessor<Object> processor = mock(ExportProcessor.class);
        coordinator.submitExport(
                new FileTaskCoordinator.ExportCommand(TaskModuleEnum.SALARY,
                        "salary.xlsx", "202608", 5), processor);

        submitted.get().run();
        verify(fileTaskEngine).runExport(9L, processor, "202608", "salary.xlsx");
    }
}
