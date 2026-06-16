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
@TableName("agent_message")
public class AgentMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    /** user / assistant / tool */
    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    /** 工具名称（仅 tool role） */
    @TableField("tool_name")
    private String toolName;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
