package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import com.qiujie.enums.LeaveEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>
 * 请假表
 * </p>
 *
 * @author qiujie
 * @since 2022-03-27
 */
@Getter
@Setter
@TableName("att_leave")
@Schema(description = "Leave对象 - 请假表")
public class Leave implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "休假类型")
    @TableField("type_num")
    private LeaveEnum typeNum;

    @Schema(description = "部门id")
    @TableField("dept_id")
    private Integer deptId;

    @Schema(description = "休假天数")
    @TableField("days")
    private Integer days;

    @Schema(description = "0禁用，1正常，默认1")
    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @Schema(description = "逻辑删除，0未删除，1删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;

}
