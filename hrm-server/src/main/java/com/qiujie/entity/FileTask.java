package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.enums.TaskTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@Accessors(chain = true)
@TableName("file_task")
@ApiModel(value = "FileTask", description = "导入导出文件任务")
public class FileTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("任务类型：IMPORT / EXPORT")
    @TableField("task_type")
    private TaskTypeEnum taskType;

    @ApiModelProperty("业务模块")
    @TableField("module")
    private TaskModuleEnum module;

    @ApiModelProperty("任务状态")
    @TableField("status")
    private TaskStatusEnum status;

    @TableField("file_name")
    private String fileName;

    @TableField("source_file_path")
    private String sourceFilePath;

    @TableField("result_file_path")
    private String resultFilePath;

    @TableField("error_file_path")
    private String errorFilePath;

    @TableField("query_params")
    private String queryParams;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("processed_count")
    private Integer processedCount;

    @TableField("success_count")
    private Integer successCount;

    @TableField("fail_count")
    private Integer failCount;

    @TableField("fail_reason")
    private String failReason;

    @TableField("operator_id")
    private Integer operatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("start_time")
    private Timestamp startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("finish_time")
    private Timestamp finishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("update_time")
    private Timestamp updateTime;
}
