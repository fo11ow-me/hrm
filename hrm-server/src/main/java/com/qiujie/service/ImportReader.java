package com.qiujie.service;

import com.qiujie.entity.FileTaskError;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * 文件任务导入读取端口。
 * <p>
 * 读取器只负责把源文件转换为批次，不参与任务状态、进度或错误文件结算。
 * 具体读取技术（EasyExcel 标准映射或灵活表头映射）由适配器隐藏。
 * </p>
 *
 * @param <T> Excel 行 DTO 类型
 */
public interface ImportReader<T> {

    /**
     * 读取源文件并发出数据批次。
     *
     * @param sourceFile   已解析到本地的源文件
     * @param taskId       文件任务 ID，用于给解析错误补充归属
     * @param batchConsumer 批次回调
     */
    void read(File sourceFile, Long taskId, Consumer<ImportBatch<T>> batchConsumer);

    /**
     * 一个导入批次，同时承载解析成功的行和解析失败的错误。
     */
    record ImportBatch<T>(List<T> rows, List<FileTaskError> errors) {
        public ImportBatch {
            rows = rows == null ? List.of() : rows;
            errors = errors == null ? List.of() : errors;
        }
    }
}
