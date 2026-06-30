package com.qiujie.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话摘要上下文实体 (MySQL)
 *
 * @author qiujie
 */
@Data
@Accessors(chain = true)
@TableName("assistant_session_context")
public class AssistantSessionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    /** 摘要文本 */
    @TableField("summary_text")
    private String summaryText;

    /** 乐观锁版本号 */
    @TableField("context_version")
    private Long contextVersion;

    /** 被摘要的源消息最大 ID */
    @TableField("source_message_id")
    private Long sourceMessageId;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
