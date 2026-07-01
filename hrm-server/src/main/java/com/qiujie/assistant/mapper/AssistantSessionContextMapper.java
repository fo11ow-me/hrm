package com.qiujie.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.assistant.entity.AssistantSessionContext;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 助手会话上下文 Mapper
 *
 * @author quuj
 */
@Mapper
public interface AssistantSessionContextMapper extends BaseMapper<AssistantSessionContext> {
}
