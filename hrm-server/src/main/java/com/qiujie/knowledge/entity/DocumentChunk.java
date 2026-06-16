package com.qiujie.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档切片实体 (PostgreSQL)
 */
@Data
@Accessors(chain = true)
@TableName("document_chunk")
public class DocumentChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("document_id")
    private Long documentId;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("chunk_text")
    private String chunkText;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
