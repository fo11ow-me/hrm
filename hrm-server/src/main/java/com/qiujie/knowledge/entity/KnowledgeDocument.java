package com.qiujie.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档实体 (MySQL)
 *
 * @author quuj
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

    @TableField("status")
    private String status;

    @TableField("failure_reason")
    private String failureReason;

    /** 文档预览文本 */
    @TableField("preview_text")
    private String previewText;

    /** 上传完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("upload_time")
    private LocalDateTime uploadTime;

    /** 处理完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("process_time")
    private LocalDateTime processTime;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("staff_id")
    private Integer staffId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    private Integer isDeleted;
}
