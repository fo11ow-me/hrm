package com.qiujie.knowledge.mapper;

import com.qiujie.knowledge.entity.DocumentChunk;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * 文档切片数据访问 — PostgreSQL (kb 数据源)。
 * 不使用 MyBatis-Plus（避免路由到 MySQL master），直接用 JdbcTemplate。
 */
@Component
public class DocumentChunkMapper {

    private final JdbcTemplate jdbc;

    public DocumentChunkMapper(@Qualifier("kbDataSource") DataSource kbDataSource) {
        this.jdbc = new JdbcTemplate(kbDataSource);
    }

    private static final RowMapper<DocumentChunk> ROW_MAPPER = (rs, rowNum) -> new DocumentChunk()
            .setId(rs.getLong("id"))
            .setDocumentId(rs.getLong("document_id"))
            .setChunkIndex(rs.getInt("chunk_index"))
            .setChunkText(rs.getString("chunk_text"))
            .setTokenCount(rs.getInt("token_count"));

    public DocumentChunk insert(DocumentChunk chunk) {
        jdbc.update(
                "INSERT INTO document_chunk (document_id, chunk_index, chunk_text, token_count) VALUES (?, ?, ?, ?)",
                chunk.getDocumentId(), chunk.getChunkIndex(), chunk.getChunkText(), chunk.getTokenCount());
        Long id = jdbc.queryForObject("SELECT currval(pg_get_serial_sequence('document_chunk', 'id'))", Long.class);
        chunk.setId(id);
        return chunk;
    }

    public int deleteByDocumentId(Long documentId) {
        return jdbc.update("DELETE FROM document_chunk WHERE document_id = ?", documentId);
    }

    public List<DocumentChunk> selectByDocumentIdOrderByChunkIndex(Long documentId) {
        return jdbc.query(
                "SELECT id, document_id, chunk_index, chunk_text, token_count FROM document_chunk WHERE document_id = ? ORDER BY chunk_index",
                ROW_MAPPER, documentId);
    }
}
