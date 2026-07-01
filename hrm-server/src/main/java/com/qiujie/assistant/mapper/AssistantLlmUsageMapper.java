package com.qiujie.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.assistant.entity.AssistantLlmUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * LLM 调用统计 Mapper
 *
 * @author quuj
 */
@Mapper
public interface AssistantLlmUsageMapper extends BaseMapper<AssistantLlmUsage> {
}
