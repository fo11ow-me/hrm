package com.qiujie.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.qiujie.entity.FileTaskError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 基于 EasyExcel 标准 DTO 映射的流式导入读取器。
 *
 * @param <T> Excel 行 DTO 类型
 */
public final class EasyExcelImportReader<T> implements ImportReader<T> {

    private static final int BATCH_SIZE = 500;

    private final Class<T> rowClass;
    private final int headRowNumber;

    public EasyExcelImportReader(Class<T> rowClass, int headRowNumber) {
        this.rowClass = rowClass;
        this.headRowNumber = headRowNumber;
    }

    @Override
    public void read(File sourceFile, Long taskId, Consumer<ImportBatch<T>> batchConsumer) {
        List<T> rows = new ArrayList<>(BATCH_SIZE);
        EasyExcel.read(sourceFile, rowClass, new AnalysisEventListener<T>() {
            @Override
            public void invoke(T data, AnalysisContext context) {
                rows.add(data);
                if (rows.size() >= BATCH_SIZE) {
                    emit(rows, batchConsumer);
                    rows.clear();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                if (!rows.isEmpty()) {
                    emit(rows, batchConsumer);
                    rows.clear();
                }
            }
        }).headRowNumber(headRowNumber).sheet().doRead();
    }

    private void emit(List<T> rows, Consumer<ImportBatch<T>> batchConsumer) {
        batchConsumer.accept(new ImportBatch<>(new ArrayList<>(rows), List.of()));
    }
}
