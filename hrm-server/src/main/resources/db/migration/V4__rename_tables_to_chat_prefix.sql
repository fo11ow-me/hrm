-- V4: Chat 前缀重命名——统一数据库表名
RENAME TABLE assistant_session         TO ast_chat_session;
RENAME TABLE assistant_message         TO ast_chat_message;
RENAME TABLE assistant_session_context TO ast_chat_session_context;
RENAME TABLE assistant_llm_usage       TO ast_chat_llm_usage;
