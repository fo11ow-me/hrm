package com.qiujie.filetask;

import com.qiujie.service.ExportProcessor;
import com.qiujie.service.ImportProcessor;
import com.qiujie.service.ImportReader;

/**
 * 业务模块提交异步文件任务的唯一入口。
 * 任务生命周期、文件介质和通知细节由实现隐藏。
 */
public interface AsyncFileTasks {

    <T> TaskSnapshot submitImport(ImportRequest<T> request);

    <T> TaskSnapshot submitExport(ExportRequest<T> request);

    record ImportRequest<T>(com.qiujie.enums.TaskModuleEnum module,
                            String fileName,
                            String sourceFilePath,
                            String queryParams,
                            Integer operatorId,
                            ImportProcessor<T> processor,
                            ImportReader<T> reader) {

        public ImportRequest(com.qiujie.enums.TaskModuleEnum module,
                             String fileName,
                             String sourceFilePath,
                             String queryParams,
                             Integer operatorId,
                             ImportProcessor<T> processor) {
            this(module, fileName, sourceFilePath, queryParams, operatorId, processor, null);
        }
    }

    record ExportRequest<T>(com.qiujie.enums.TaskModuleEnum module,
                            String fileName,
                            String queryParams,
                            Integer operatorId,
                            ExportProcessor<T> processor) {
    }
}
