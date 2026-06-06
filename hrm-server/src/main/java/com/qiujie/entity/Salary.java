package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * <p>
 *
 * </p>
 *
 * @author qiujie
 * @since 2022-04-06
 */
@Getter
@Setter
@TableName("sal_salary")
@ApiModel(value = "Salary对象", description = "")
public class Salary implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("员工id")
    @TableField("staff_id")
    private Integer staffId;

    @ApiModelProperty("基础工资")
    @TableField("base_salary")
    private BigDecimal baseSalary;

    @ApiModelProperty("平均日薪")
    @TableField("day_salary")
    private BigDecimal daySalary;

    @ApiModelProperty("平均时薪资")
    @TableField("hour_salary")
    private BigDecimal hourSalary;

    @ApiModelProperty("加班费")
    @TableField("overtime_salary")
    private BigDecimal overtimeSalary;

    @ApiModelProperty("生活补贴")
    @TableField("subsidy")
    private BigDecimal subsidy;

    @ApiModelProperty("奖金")
    @TableField("bonus")
    private BigDecimal bonus;

    @ApiModelProperty("月份")
    @TableField("month")
    private String month;

    @ApiModelProperty("早退扣款")
    @TableField("late_deduct")
    private BigDecimal lateDeduct;

    @ApiModelProperty("休假扣款")
    @TableField("leave_deduct")
    private BigDecimal leaveDeduct;

    @ApiModelProperty("早退扣款")
    @TableField("leave_early_deduct")
    private BigDecimal leaveEarlyDeduct;

    @ApiModelProperty("旷工扣款")
    @TableField("absenteeism_deduct")
    private BigDecimal absenteeismDeduct;

    @ApiModelProperty("总工资")
    @TableField("total_salary")
        private BigDecimal totalSalary;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @ApiModelProperty("逻辑删除，0未删除，1删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;


}
