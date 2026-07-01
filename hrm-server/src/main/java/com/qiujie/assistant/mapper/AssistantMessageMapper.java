package com.qiujie.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.assistant.entity.AssistantMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 助手消息 Mapper
 *
 * @author quuj
 */
@Mapper
public interface AssistantMessageMapper extends BaseMapper<AssistantMessage> {

    @Delete("DELETE FROM assistant_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(Long sessionId);
}
