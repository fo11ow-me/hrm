package com.qiujie.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * LLM 调用统计实体 (MySQL)
 *
 * @author quuj
 */
@Data
@TableName("ast_chat_llm_usage")
public class ChatLlmUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("staff_id")
    private Integer staffId;

    /** ASSISTANT / QA */
    @TableField("module")
    private String module;

    @TableField("endpoint")
    private String endpoint;

    @TableField("session_id")
    private Long sessionId;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("is_estimated")
    private Boolean isEstimated;

    @TableField("cost_amount")
    private BigDecimal costAmount;

    @TableField("cost_currency")
    private String costCurrency;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("success")
    private Boolean success;

    @TableField("error_message")
    private String errorMessage;

    @TableField("model_name")
    private String modelName;

    @TableField("create_time")
    private LocalDateTime createTime;
}
