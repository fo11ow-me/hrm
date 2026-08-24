package com.qiujie.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库问答记录 (PostgreSQL kb_qa_record)
 */
@Data
@TableName("kb_qa_record")
public class KbQaRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String question;

    private String answer;

    @TableField("staff_id")
    private Integer staffId;

    @TableField("evidence_level")
    private String evidenceLevel;

    private Boolean answered;

    @TableField("citation_count")
    private Integer citationCount;

    private String endpoint;

    private Boolean success;

    @TableField("create_time")
    private LocalDateTime createTime;
}