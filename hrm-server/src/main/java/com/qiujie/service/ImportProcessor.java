package com.qiujie.service;

import com.qiujie.entity.FileTaskError;
import com.qiujie.enums.TaskModuleEnum;

import java.util.List;
import java.util.function.Consumer;

/**
 * 异步导入处理器接口。
 * 每个需要异步导入能力的模块实现此接口，定义自己的批量校验与入库逻辑。
 * FileTaskEngine 负责流式读取、缓冲管理、进度更新和错误文件生成。
 *
 * @param <T> Excel 行对应的 DTO 类型
 * @author qiujie
 */
public interface ImportProcessor<T> {

    /**
     * Excel 行映射的 DTO 类型
     */
    Class<T> getRowClass();

    /**
     * 表头行数（从 1 开始），默认第 2 行为表头
     */
    default int headRowNumber() {
        return 2;
    }

    /**
     * 所属业务模块
     */
    TaskModuleEnum getModule();

    /**
     * 批量处理导入行：校验 + 转换 + 入库。
     * 采用批量回调而非逐行回调，允许实现方在批次内做批量预加载（如一次性查出本批涉及的员工/部门），避免 N+1 查询。
     * 校验失败的行通过 errorCollector 上报，引擎负责持久化到 file_task_error 并生成错误文件。
     *
     * @param rows           本批次 Excel 行数据
     * @param taskId         文件任务ID
     * @param errorCollector 错误收集器，校验失败时调用 errorCollector.accept(error)
     */
    void processBatch(List<T> rows, Long taskId, Consumer<FileTaskError> errorCollector);
}
