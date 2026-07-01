package com.qiujie.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 会话实体 (MySQL)
 */
@Data
@Accessors(chain = true)
@TableName("ast_chat_session")
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("staff_id")
    private Integer staffId;

    @TableField("title")
    private String title;

    /** CHAT / KB_SEARCH */
    @TableField("mode")
    private String mode;

    @TableField("status")
    private String status;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("message_count")
    private Integer messageCount;

    @TableField("total_tokens")
    private Long totalTokens;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
