package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Salary对象 - ")
public class Salary implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "员工id")
    @TableField("staff_id")
    private Integer staffId;

    @Schema(description = "基础工资")
    @TableField("base_salary")
    private BigDecimal baseSalary;

    @Schema(description = "平均日薪")
    @TableField("day_salary")
    private BigDecimal daySalary;

    @Schema(description = "平均时薪资")
    @TableField("hour_salary")
    private BigDecimal hourSalary;

    @Schema(description = "加班费")
    @TableField("overtime_salary")
    private BigDecimal overtimeSalary;

    @Schema(description = "生活补贴")
    @TableField("subsidy")
    private BigDecimal subsidy;

    @Schema(description = "奖金")
    @TableField("bonus")
    private BigDecimal bonus;

    @Schema(description = "月份")
    @TableField("month")
    private String month;

    @Schema(description = "早退扣款")
    @TableField("late_deduct")
    private BigDecimal lateDeduct;

    @Schema(description = "休假扣款")
    @TableField("leave_deduct")
    private BigDecimal leaveDeduct;

    @Schema(description = "早退扣款")
    @TableField("leave_early_deduct")
    private BigDecimal leaveEarlyDeduct;

    @Schema(description = "旷工扣款")
    @TableField("absenteeism_deduct")
    private BigDecimal absenteeismDeduct;

    @Schema(description = "总工资")
    @TableField("total_salary")
        private BigDecimal totalSalary;

    @Schema(description = "备注")
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
