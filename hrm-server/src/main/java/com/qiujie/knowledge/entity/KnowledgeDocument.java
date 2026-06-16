package com.qiujie.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档实体 (MySQL)
 */
@Data
@Accessors(chain = true)
@TableName("kb_document")
public class KnowledgeDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("old_name")
    private String oldName;

    @TableField("type")
    private String type;

    @TableField("file_hash")
    private String fileHash;

    @TableField("file_size")
    private Long fileSize;

    @TableField("stored_size")
    private Long storedSize;

    @TableField("compressed")
    private Integer compressed;

    @TableField("status")
    private String status;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("staff_id")
    private Integer staffId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
