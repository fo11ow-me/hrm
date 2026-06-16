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
@TableName("agent_session")
public class AgentSession implements Serializable {

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

    /** L1 会话记忆：LLM 增量摘要 */
    @TableField("session_memory")
    private String sessionMemory;

    /** L2 紧凑摘要：精炼的历史压缩 */
    @TableField("compact_summary")
    private String compactSummary;

    @TableField("message_count")
    private Integer messageCount;

    @TableField("total_tokens")
    private Long totalTokens;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
