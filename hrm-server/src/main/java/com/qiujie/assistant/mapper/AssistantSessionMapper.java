package com.qiujie.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.assistant.entity.AssistantSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 助手会话 Mapper
 *
 * @author quuj
 */
@Mapper
public interface AssistantSessionMapper extends BaseMapper<AssistantSession> {
}
