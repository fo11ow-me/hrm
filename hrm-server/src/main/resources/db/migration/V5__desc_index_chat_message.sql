-- V5: 显式降序索引——匹配 ORDER BY create_time DESC 查询方向
ALTER TABLE ast_chat_message DROP INDEX idx_msg_session;
ALTER TABLE ast_chat_message ADD INDEX idx_msg_session (session_id ASC, create_time DESC);
