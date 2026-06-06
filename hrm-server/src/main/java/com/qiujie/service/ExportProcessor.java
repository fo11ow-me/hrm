package com.qiujie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.enums.TaskModuleEnum;

/**
 * 异步导出处理器接口。
 * 每个需要异步导出能力的模块实现此接口，定义分页查询逻辑。
 * FileTaskEngine 负责流式写入、进度更新和文件管理。
 *
 * @param <T> Excel 行对应的 DTO/VO 类型
 * @author qiujie
 */
public interface ExportProcessor<T> {

    /**
     * Excel 行映射的 DTO/VO 类型
     */
    Class<T> getRowClass();

    /**
     * 所属业务模块
     */
    TaskModuleEnum getModule();

    /**
     * 分页查询导出数据
     *
     * @param current          当前页码
     * @param pageSize         每页条数
     * @param queryParamsJson  查询参数 JSON（由前端传入，如月份等）
     * @return 分页结果
     */
    IPage<T> queryPage(int current, int pageSize, String queryParamsJson);
}
