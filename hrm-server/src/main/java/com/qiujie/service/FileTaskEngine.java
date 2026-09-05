package com.qiujie.service;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.entity.FileTask;
import com.qiujie.entity.FileTaskError;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.filetask.ArtifactStore;
import com.qiujie.filetask.TaskRepository;
import com.qiujie.storage.MinioStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用异步导入导出引擎。
 * 负责流式读写、缓冲管理、进度更新、错误文件生成等通用逻辑，
 * 具体业务的校验和入库逻辑由各模块通过 ImportProcessor / ExportProcessor 接口提供。
 *
 * @author qiujie
 */
@Service
public class FileTaskEngine {

    private static final int DB_BATCH_SIZE = 200;
    private static final int EXPORT_PAGE_SIZE = 500;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FileTaskErrorService fileTaskErrorService;

    @Autowired
    private ArtifactStore artifactStore;

    @Autowired
    private MinioStorageService storageService;

    /**
     * 执行异步导入。
     * 使用默认读取器流式读取，每攒够一批行交给 processor 批量处理。
     *
     * @param taskId    文件任务ID
     * @param processor 模块导入处理器
     */
    public <T> void runImport(Long taskId, ImportProcessor<T> processor) {
        runImport(taskId, processor,
                new EasyExcelImportReader<>(processor.getRowClass(), processor.headRowNumber()));
    }

    /**
     * 使用指定读取器执行异步导入。
     * 读取器负责文件格式适配，处理器只负责业务校验和入库，任务结算统一在此完成。
     */
    public <T> void runImport(Long taskId, ImportProcessor<T> processor,
                              ImportReader<T> reader) {
        if (!taskRepository.claimRunning(taskId)) {
            return;
        }
        FileTask task = taskRepository.getById(taskId);
        if (task == null) {
            return;
        }
        File sourceFile = null;
        boolean temporarySource = false;
        try {
            sourceFile = artifactStore.resolveSource(task.getSourceFilePath());
            temporarySource = artifactStore.isRemote(task.getSourceFilePath());
            reader.read(sourceFile, taskId,
                    batch -> processBatch(batch, taskId, processor));

            FileTask finishedTask = taskRepository.getById(taskId);
            if (finishedTask != null && finishedTask.getProcessedCount() != null) {
                // 流式读取完成后回填真实总量，前端进度显示准确
                taskRepository.setTotalCount(taskId, finishedTask.getProcessedCount());
            }
            if (finishedTask != null && finishedTask.getFailCount() != null && finishedTask.getFailCount() > 0) {
                taskRepository.generateErrorFile(taskId);
                taskRepository.finish(taskId, TaskStatusEnum.PARTIAL_SUCCESS);
            } else {
                // 导入完全成功时立即删除源文件，避免敏感数据长期驻留磁盘
                taskRepository.deleteSourceFile(taskId);
                taskRepository.finish(taskId, TaskStatusEnum.SUCCESS);
            }
        } catch (Exception e) {
            taskRepository.fail(taskId, e);
        } finally {
            deleteTemporaryFile(sourceFile, temporarySource);
        }
    }

    private <T> void processBatch(ImportReader.ImportBatch<T> batch,
                                  Long taskId, ImportProcessor<T> processor) {
        List<FileTaskError> errors = new ArrayList<>(batch.errors());
        int parsedRows = batch.rows().size();
        if (!batch.rows().isEmpty()) {
            processor.processBatch(batch.rows(), taskId, errors::add);
        }
        if (!errors.isEmpty()) {
            fileTaskErrorService.saveBatch(errors, DB_BATCH_SIZE);
        }
        int businessFailures = errors.size() - batch.errors().size();
        taskRepository.increaseProgress(taskId, 0, parsedRows + batch.errors().size(),
                parsedRows - businessFailures, errors.size());
    }

    /**
     * 执行异步导出。
     * 通过 processor 分页查询数据，增量写入 Excel，每页更新进度。
     *
     * @param taskId           文件任务ID
     * @param processor        模块导出处理器
     * @param queryParamsJson  查询参数 JSON
     * @param exportName       导出文件名
     */
    public <T> void runExport(Long taskId, ExportProcessor<T> processor, String queryParamsJson, String exportName) {
        if (!taskRepository.claimRunning(taskId)) {
            return;
        }
        File resultFile = null;
        ExcelWriter excelWriter = null;
        try {
            resultFile = artifactStore.createTaskFile("task-result", exportName);
            excelWriter = EasyExcel.write(resultFile, processor.getRowClass()).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("data").build();
            int current = 1;
            IPage<T> page;
            do {
                page = processor.queryPage(current, EXPORT_PAGE_SIZE, queryParamsJson);
                List<T> list = page.getRecords();
                if (list.isEmpty()) {
                    break;
                }
                excelWriter.write(list, writeSheet);
                if (current == 1) {
                    taskRepository.setTotalCount(taskId, (int) page.getTotal());
                }
                taskRepository.increaseProgress(taskId, 0, list.size(), list.size(), 0);
                current++;
            } while (current <= page.getPages());
            taskRepository.setResultFile(taskId, artifactStore.upload(resultFile, "task-result"));
            resultFile = null;
            taskRepository.finish(taskId, TaskStatusEnum.SUCCESS);
        } catch (Exception e) {
            taskRepository.fail(taskId, e);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
            deleteTemporaryFile(resultFile, true);
        }
    }

    private void deleteTemporaryFile(File file, boolean temporary) {
        if (temporary && file != null && file.isFile() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    /**
     * 解析源文件已委托给 ArtifactStore。
     */
}
