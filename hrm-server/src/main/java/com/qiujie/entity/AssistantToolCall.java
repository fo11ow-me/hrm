package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@Accessors(chain = true)
@TableName("assistant_tool_call")
public class AssistantToolCall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("message_id")
    private Long messageId;

    @TableField("staff_id")
    private Integer staffId;

    @TableField("intent")
    private String intent;

    @TableField("tool_name")
    private String toolName;

    @TableField("arguments_json")
    private String argumentsJson;

    @TableField("result_json")
    private String resultJson;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("create_time")
    private Timestamp createTime;
}
