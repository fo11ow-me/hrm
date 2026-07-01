-- V3: 三级记忆压缩——字段扩展 + 存量清零
ALTER TABLE assistant_session_context
  ADD COLUMN session_memory_base_message_id BIGINT COMMENT 'L1 覆盖的起始消息ID',
  ADD COLUMN session_memory_range_end_message_id BIGINT COMMENT 'L1 覆盖的结束消息ID',
  ADD COLUMN compact_summary_base_message_id BIGINT COMMENT 'L2 覆盖的起始消息ID',
  ADD COLUMN compact_summary_range_end_message_id BIGINT COMMENT 'L2 覆盖的结束消息ID',
  ADD COLUMN summary_text TEXT COMMENT '前端展示用非LLM会话摘要',
  ADD COLUMN source_message_id BIGINT COMMENT 'summary_text 覆盖的结束消息ID';

-- 存量清零：让已有会话从零重新压缩
UPDATE assistant_session_context
SET session_memory = NULL,
    compact_summary = NULL,
    context_version = 0;
