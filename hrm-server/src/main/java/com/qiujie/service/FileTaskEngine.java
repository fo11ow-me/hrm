package com.qiujie.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.entity.FileTask;
import com.qiujie.entity.FileTaskError;
import com.qiujie.enums.TaskStatusEnum;
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

    private static final int IMPORT_BATCH_SIZE = 500;
    private static final int DB_BATCH_SIZE = 200;
    private static final int EXPORT_PAGE_SIZE = 500;

    @Autowired
    private FileTaskService fileTaskService;

    @Autowired
    private FileTaskErrorService fileTaskErrorService;

    /**
     * 执行异步导入。
     * 使用 EasyExcel 流式读取，每攒够 IMPORT_BATCH_SIZE 行交给 processor 批量处理，
     * 处理完清空 buffer 回收内存。
     *
     * @param taskId    文件任务ID
     * @param processor 模块导入处理器
     */
    public <T> void runImport(Long taskId, ImportProcessor<T> processor) {
        fileTaskService.markRunning(taskId);
        FileTask task = fileTaskService.getById(taskId);
        if (task == null) {
            return;
        }
        List<T> buffer = new ArrayList<>(IMPORT_BATCH_SIZE);
        try {
            EasyExcel.read(task.getSourceFilePath(), processor.getRowClass(),
                    new AnalysisEventListener<T>() {
                        @Override
                        public void invoke(T data, AnalysisContext context) {
                            buffer.add(data);
                            if (buffer.size() >= IMPORT_BATCH_SIZE) {
                                processBuffer(new ArrayList<>(buffer), taskId, processor);
                                buffer.clear();
                            }
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            if (!buffer.isEmpty()) {
                                processBuffer(new ArrayList<>(buffer), taskId, processor);
                                buffer.clear();
                            }
                        }
                    }).headRowNumber(processor.headRowNumber()).sheet().doRead();

            FileTask finishedTask = fileTaskService.getById(taskId);
            if (finishedTask != null && finishedTask.getFailCount() != null && finishedTask.getFailCount() > 0) {
                fileTaskService.generateErrorFile(taskId);
                fileTaskService.finish(taskId, TaskStatusEnum.PARTIAL_SUCCESS);
            } else {
                fileTaskService.finish(taskId, TaskStatusEnum.SUCCESS);
            }
        } catch (Exception e) {
            fileTaskService.fail(taskId, e);
        }
    }

    private <T> void processBuffer(List<T> buffer, Long taskId, ImportProcessor<T> processor) {
        List<FileTaskError> errors = new ArrayList<>();
        processor.processBatch(buffer, taskId, errors::add);
        if (!errors.isEmpty()) {
            fileTaskErrorService.saveBatch(errors, DB_BATCH_SIZE);
        }
        fileTaskService.increaseProgress(taskId, buffer.size(), buffer.size(),
                buffer.size() - errors.size(), errors.size());
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
        fileTaskService.markRunning(taskId);
        File resultFile = fileTaskService.buildTaskFile("task-result", exportName);
        ExcelWriter excelWriter = EasyExcel.write(resultFile, processor.getRowClass()).build();
        WriteSheet writeSheet = EasyExcel.writerSheet("data").build();
        try {
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
                    fileTaskService.setTotalCount(taskId, (int) page.getTotal());
                }
                fileTaskService.increaseProgress(taskId, 0, list.size(), list.size(), 0);
                current++;
            } while (current <= page.getPages());
            fileTaskService.setResultFile(taskId, resultFile.getAbsolutePath());
            fileTaskService.finish(taskId, TaskStatusEnum.SUCCESS);
        } catch (Exception e) {
            fileTaskService.fail(taskId, e);
        } finally {
            excelWriter.finish();
        }
    }
}
