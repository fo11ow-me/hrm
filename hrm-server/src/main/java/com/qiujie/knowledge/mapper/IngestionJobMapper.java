package com.qiujie.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.knowledge.entity.IngestionJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档摄入异步任务 Mapper
 *
 * @author quuj
 */
@Mapper
public interface IngestionJobMapper extends BaseMapper<IngestionJob> {
}
