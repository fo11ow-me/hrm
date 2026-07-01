package com.qiujie.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 消息实体 (MySQL)
 */
@Data
@Accessors(chain = true)
@TableName("assistant_message")
public class AgentMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    /** user / assistant / tool */
    @TableField("role")
    private String role;

    @TableField("tool_mode")
    private String toolMode;

    @TableField("content")
    private String content;

    @TableField("structured_payload")
    private String structuredPayload;

    @TableField("create_time")
    private LocalDateTime createTime;
}
