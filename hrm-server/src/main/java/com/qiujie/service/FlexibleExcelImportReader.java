package com.qiujie.service;

import com.qiujie.entity.FileTaskError;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.util.FlexibleExcelImporter;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 基于表头映射的灵活 Excel 导入读取器。
 * <p>
 * 解析结果按批次发出，解析失败行不会中断后续业务行，而是交给统一任务引擎结算。
 * </p>
 *
 * @param <T> Excel 行 DTO 类型
 */
public final class FlexibleExcelImportReader<T> implements ImportReader<T> {

    private static final int BATCH_SIZE = 500;

    private final int headRowNumber;
    private final TaskModuleEnum module;
    private final Class<T> rowClass;
    private final FlexibleExcelImporter.AiHeaderMatcher fallback;

    public FlexibleExcelImportReader(int headRowNumber,
                                     TaskModuleEnum module,
                                     Class<T> rowClass,
                                     FlexibleExcelImporter.AiHeaderMatcher fallback) {
        this.headRowNumber = headRowNumber;
        this.module = module;
        this.rowClass = rowClass;
        this.fallback = fallback;
    }

    @Override
    public void read(File sourceFile, Long taskId, Consumer<ImportBatch<T>> batchConsumer) {
        List<T> rows = new ArrayList<>(BATCH_SIZE);
        List<FileTaskError> errors = new ArrayList<>();
        try (FileInputStream input = new FileInputStream(sourceFile)) {
            FlexibleExcelImporter.read(input, headRowNumber, module, rowClass, fallback,
                    result -> {
                        if (result.isSuccess()) {
                            rows.add(result.getEntity());
                        } else {
                            errors.add(new FileTaskError()
                                    .setTaskId(taskId)
                                    .setRowNum(result.getRowNum())
                                    .setErrorMessage(result.getError() == null ? "解析失败" : result.getError()));
                        }
                        if (rows.size() >= BATCH_SIZE || errors.size() >= BATCH_SIZE) {
                            emit(rows, errors, batchConsumer);
                            rows.clear();
                            errors.clear();
                        }
                    });
            if (!rows.isEmpty() || !errors.isEmpty()) {
                emit(rows, errors, batchConsumer);
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel 文件解析失败", e);
        }
    }

    private void emit(List<T> rows, List<FileTaskError> errors,
                      Consumer<ImportBatch<T>> batchConsumer) {
        batchConsumer.accept(new ImportBatch<>(new ArrayList<>(rows), new ArrayList<>(errors)));
    }
}
