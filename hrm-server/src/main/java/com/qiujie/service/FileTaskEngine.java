package com.qiujie.service;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.entity.FileTask;
import com.qiujie.entity.FileTaskError;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.storage.MinioStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
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
    private FileTaskRuntimePort fileTaskRuntime;

    @Autowired
    private FileTaskErrorService fileTaskErrorService;

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
        if (!fileTaskRuntime.claimRunning(taskId)) {
            return;
        }
        FileTask task = fileTaskRuntime.getById(taskId);
        if (task == null) {
            return;
        }
        File sourceFile = null;
        boolean temporarySource = false;
        try {
            sourceFile = resolveSourceFile(task.getSourceFilePath());
            temporarySource = isDownloadedSource(task.getSourceFilePath());
            reader.read(sourceFile, taskId,
                    batch -> processBatch(batch, taskId, processor));

            FileTask finishedTask = fileTaskRuntime.getById(taskId);
            if (finishedTask != null && finishedTask.getProcessedCount() != null) {
                // 流式读取完成后回填真实总量，前端进度显示准确
                fileTaskRuntime.setTotalCount(taskId, finishedTask.getProcessedCount());
            }
            if (finishedTask != null && finishedTask.getFailCount() != null && finishedTask.getFailCount() > 0) {
                fileTaskRuntime.generateErrorFile(taskId);
                fileTaskRuntime.finish(taskId, TaskStatusEnum.PARTIAL_SUCCESS);
            } else {
                // 导入完全成功时立即删除源文件，避免敏感数据长期驻留磁盘
                fileTaskRuntime.deleteSourceFile(taskId);
                fileTaskRuntime.finish(taskId, TaskStatusEnum.SUCCESS);
            }
        } catch (Exception e) {
            fileTaskRuntime.fail(taskId, e);
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
        fileTaskRuntime.increaseProgress(taskId, 0, parsedRows + batch.errors().size(),
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
        if (!fileTaskRuntime.claimRunning(taskId)) {
            return;
        }
        File resultFile = null;
        ExcelWriter excelWriter = null;
        try {
            resultFile = fileTaskRuntime.buildTaskFile("task-result", exportName);
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
                    fileTaskRuntime.setTotalCount(taskId, (int) page.getTotal());
                }
                fileTaskRuntime.increaseProgress(taskId, 0, list.size(), list.size(), 0);
                current++;
            } while (current <= page.getPages());
            fileTaskRuntime.setResultFile(taskId, fileTaskRuntime.uploadToMinio(resultFile, "task-result"));
            resultFile = null;
            fileTaskRuntime.finish(taskId, TaskStatusEnum.SUCCESS);
        } catch (Exception e) {
            fileTaskRuntime.fail(taskId, e);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
            deleteTemporaryFile(resultFile, true);
        }
    }

    private boolean isDownloadedSource(String path) {
        return path != null && !path.contains(File.separator) && !path.startsWith("/");
    }

    private void deleteTemporaryFile(File file, boolean temporary) {
        if (temporary && file != null && file.isFile() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    /**
     * 解析源文件。若为 MinIO key 则下载到临时文件，否则直接返回本地文件引用。
     */
    private File resolveSourceFile(String path) {
        if (path == null || path.contains(File.separator) || path.startsWith("/")) {
            return new File(path);
        }
        File tempFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "hrm", path);
        tempFile.getParentFile().mkdirs();
        try (InputStream in = storageService.get(path)) {
            FileUtil.writeFromStream(in, tempFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download source file from MinIO: " + path, e);
        }
        return tempFile;
    }
}
