package com.qiujie.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 助手会话上下文实体 (MySQL)
 * 存储 L1 会话记忆、L2 紧凑摘要、前端展示摘要及压缩范围追踪
 *
 * @author quuj
 */
@Data
@TableName("assistant_session_context")
public class AssistantSessionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("session_id")
    private Long sessionId;

    /** L1 会话记忆摘要 */
    @TableField("session_memory")
    private String sessionMemory;

    /** L2 紧凑摘要 */
    @TableField("compact_summary")
    private String compactSummary;

    /** L1 覆盖的起始消息ID */
    @TableField("session_memory_base_message_id")
    private Long sessionMemoryBaseMessageId;

    /** L1 覆盖的结束消息ID */
    @TableField("session_memory_range_end_message_id")
    private Long sessionMemoryRangeEndMessageId;

    /** L2 覆盖的起始消息ID */
    @TableField("compact_summary_base_message_id")
    private Long compactSummaryBaseMessageId;

    /** L2 覆盖的结束消息ID */
    @TableField("compact_summary_range_end_message_id")
    private Long compactSummaryRangeEndMessageId;

    /** 前端展示用非LLM会话摘要 */
    @TableField("summary_text")
    private String summaryText;

    /** summary_text 覆盖的结束消息ID */
    @TableField("source_message_id")
    private Long sourceMessageId;

    /** 乐观锁版本号 */
    @TableField("context_version")
    private Long contextVersion;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
