package com.qiujie.service;

import com.qiujie.entity.FileTask;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskTypeEnum;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 文件任务生命周期门面。
 * <p>
 * 业务模块只描述导入/导出的业务处理器，任务创建、异步提交和执行入口由本类统一拥有。
 * </p>
 */
@Service
public class FileTaskCoordinator {

    private final FileTaskService fileTaskService;
    private final FileTaskEngine fileTaskEngine;
    private final ThreadPoolTaskExecutor fileTaskExecutor;

    public FileTaskCoordinator(FileTaskService fileTaskService,
                               FileTaskEngine fileTaskEngine,
                               @Qualifier("fileTaskExecutor") ThreadPoolTaskExecutor fileTaskExecutor) {
        this.fileTaskService = fileTaskService;
        this.fileTaskEngine = fileTaskEngine;
        this.fileTaskExecutor = fileTaskExecutor;
    }

    /** 导入任务命令。sourceFilePath 可以是本地路径或对象存储 key。 */
    public record ImportCommand(TaskModuleEnum module,
                                String fileName,
                                String sourceFilePath,
                                String queryParams,
                                Integer operatorId) {
    }

    /** 导出任务命令。 */
    public record ExportCommand(TaskModuleEnum module,
                                String fileName,
                                String queryParams,
                                Integer operatorId) {
    }

    /** 提交结果，保留现有 FileTask 作为 API 兼容的初始快照。 */
    public record TaskSubmission(Long taskId, FileTask snapshot) {
    }

    /** 使用默认 Excel reader 提交导入任务。 */
    public <T> TaskSubmission submitImport(ImportCommand command, ImportProcessor<T> processor) {
        return submitImport(command, processor, null);
    }

    /** 使用业务指定 reader 提交导入任务。 */
    public <T> TaskSubmission submitImport(ImportCommand command,
                                           ImportProcessor<T> processor,
                                           ImportReader<T> reader) {
        FileTask task = fileTaskService.createTask(
                TaskTypeEnum.IMPORT,
                command.module(),
                command.fileName(),
                command.sourceFilePath(),
                command.queryParams(),
                command.operatorId());
        fileTaskExecutor.execute(() -> {
            if (reader == null) {
                fileTaskEngine.runImport(task.getId(), processor);
            } else {
                fileTaskEngine.runImport(task.getId(), processor, reader);
            }
        });
        return new TaskSubmission(task.getId(), task);
    }

    /** 提交导出任务。 */
    public <T> TaskSubmission submitExport(ExportCommand command, ExportProcessor<T> processor) {
        FileTask task = fileTaskService.createTask(
                TaskTypeEnum.EXPORT,
                command.module(),
                command.fileName(),
                null,
                command.queryParams(),
                command.operatorId());
        fileTaskExecutor.execute(() -> fileTaskEngine.runExport(
                task.getId(), processor, command.queryParams(), command.fileName()));
        return new TaskSubmission(task.getId(), task);
    }

    /** 订阅当前操作者的任务状态事件。 */
    public SseEmitter subscribe() {
        return fileTaskService.subscribeSse();
    }

    /** 查询当前操作者可访问的任务列表。 */
    public com.qiujie.dto.ResponseDTO list(Integer current, Integer size,
                                            String taskType, String module) {
        return fileTaskService.list(current, size, taskType, module);
    }

    /** 查询当前操作者可访问的任务详情。 */
    public com.qiujie.dto.ResponseDTO inspect(Long taskId) {
        return fileTaskService.query(taskId);
    }

    /** 查询当前操作者可访问的导入错误。 */
    public com.qiujie.dto.ResponseDTO queryErrors(Long taskId, Integer current, Integer size) {
        return fileTaskService.queryErrors(taskId, current, size);
    }

    /** 将任务文件下载到 HTTP 响应，隐藏本地文件和对象存储差异。 */
    public void download(Long taskId, String fileType, HttpServletResponse response) throws IOException {
        fileTaskService.download(taskId, fileType, response);
    }
}
