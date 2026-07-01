package com.qiujie.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 助手消息实体 (MySQL)
 *
 * @author quuj
 */
@Data
@Accessors(chain = true)
@TableName("assistant_message")
public class AssistantMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    /** USER / ASSISTANT / TOOL */
    @TableField("role")
    private String role;

    /** CHAT / KB_SEARCH */
    @TableField("tool_mode")
    private String toolMode;

    @TableField("content")
    private String content;

    /** 结构化负载（JSON），TOOL 角色时存工具名/参数/结果/状态 */
    @TableField("structured_payload")
    private String structuredPayload;

    @TableField("create_time")
    private LocalDateTime createTime;
}
