package com.qiujie.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author qiujie
 * @Date 2022/4/7
 * @Version 1.0
 */

@Data
public class AttendanceMonthVO implements Serializable {


    private static final long serialVersionUID = 1L;

    @ApiModelProperty("员工id")
    @ExcelProperty("员工id")
    private Integer staffId;

    @ApiModelProperty("部门id")
    @ExcelProperty("部门id")
    private Integer deptId;

    @ExcelProperty("员工工号")
    @ApiModelProperty("员工工号")
    private String code;

    @ExcelProperty("员工姓名")
    @ApiModelProperty("员工姓名")
    private String name;

    @ExcelProperty("电话")
    @ApiModelProperty("电话")
    private String phone;

    @ExcelProperty("地址")
    @ApiModelProperty("地址")
    private String address;

    @ExcelProperty("部门")
    @ApiModelProperty("部门")
    private String deptName;

    @ExcelProperty("迟到次数")
    private Integer lateTimes;

    @ExcelProperty("早退次数")
    private Integer leaveEarlyTimes;

    @ExcelProperty("旷工次数")
    private Integer absenteeismTimes;

    @ExcelProperty("休假天数")
    private Integer leaveDays;

    @ExcelProperty("调休天数")
    private Integer timeOffDays;
}
