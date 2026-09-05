-- ============================================================
-- HRM 知识库模块 - PostgreSQL 向量存储
-- 数据库: hrm_kb (kb 数据源)
-- ============================================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 向量存储表 (Spring AI PgVectorStore 兼容)
-- 维度: 1024 = text-embedding-v3
CREATE TABLE IF NOT EXISTS vector_store (
    id        uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    content   text,
    metadata  jsonb,
    embedding vector(1024) NOT NULL
);
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 文档切片元数据表
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
CREATE INDEX IF NOT EXISTS idx_chunk_document ON document_chunk(document_id);

-- 列注释
COMMENT ON COLUMN document_chunk.id IS '主键';
COMMENT ON COLUMN document_chunk.document_id IS '所属文档ID';
COMMENT ON COLUMN document_chunk.chunk_index IS '切片序号';
COMMENT ON COLUMN document_chunk.chunk_text IS '切片文本';
COMMENT ON COLUMN document_chunk.token_count IS 'Token数量';
COMMENT ON COLUMN document_chunk.chunk_summary IS '切片摘要';
COMMENT ON COLUMN document_chunk.char_start IS '起始字符位置';
COMMENT ON COLUMN document_chunk.char_end IS '结束字符位置';
COMMENT ON COLUMN document_chunk.metadata_json IS '切片元数据';
COMMENT ON COLUMN document_chunk.create_time IS '创建时间';

-- 知识库文档表（与 MySQL 中的 kb_document 对应，供 PostgreSQL 本地 JOIN 查询）
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

-- 知识库问答记录表
CREATE TABLE IF NOT EXISTS kb_qa_record (
    id              BIGSERIAL PRIMARY KEY,
    question        TEXT NOT NULL,
    answer          TEXT,
    staff_id        INT,
    evidence_level  VARCHAR(20),
    answered        BOOLEAN DEFAULT FALSE,
    citation_count  INT DEFAULT 0,
    endpoint        VARCHAR(100),
    success         BOOLEAN DEFAULT TRUE,
    create_time     TIMESTAMP NOT NULL DEFAULT NOW()
);
