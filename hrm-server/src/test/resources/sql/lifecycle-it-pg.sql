-- 文档生命周期集成测试：PostgreSQL/pgvector 初始化（与 sql/knowledge_base.sql 一致）
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    id        uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    content   text,
    metadata  jsonb,
    embedding vector(1024) NOT NULL
);

CREATE TABLE IF NOT EXISTS document_chunk (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT       NOT NULL,
    chunk_index     INT          NOT NULL,
    chunk_text      TEXT         NOT NULL,
    token_count     INT          DEFAULT 0,
    chunk_summary   TEXT,
    char_start      INT          DEFAULT 0,
    char_end        INT          DEFAULT 0,
    metadata_json   jsonb,
    create_time     TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS kb_document (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    old_name        VARCHAR(255),
    type            VARCHAR(50),
    file_hash       VARCHAR(128),
    file_size       BIGINT DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'PENDING',
    failure_reason  TEXT,
    preview_text    TEXT,
    upload_time     TIMESTAMP,
    process_time    TIMESTAMP,
    chunk_count     INT DEFAULT 0,
    staff_id        INT,
    create_time     TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted      INT DEFAULT 0
);
