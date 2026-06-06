package com.qiujie.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author qiujie
 * @Date 2022/4/8
 * @Version 1.0
 */

@Data
@Accessors(chain = true)
public class StaffSalaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("员工id")
    private Integer staffId;

    @ApiModelProperty("部门id")
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

    @ExcelProperty("迟到扣款")
    @ApiModelProperty("迟到扣款")
    private BigDecimal lateDeduct;

    @ExcelProperty("休假扣款")
    @ApiModelProperty("休假扣款")
    private BigDecimal leaveDeduct;

    @ExcelProperty("早退扣款")
    @ApiModelProperty("早退扣款")
    private BigDecimal leaveEarlyDeduct;

    @ExcelProperty("旷工扣款")
    @ApiModelProperty("旷工扣款")
    private BigDecimal absenteeismDeduct;

    @ExcelProperty("公积金缴纳费用")
    @ApiModelProperty("公积金缴纳费用")
    private BigDecimal housePay;

    @ExcelProperty("社保缴纳费用")
    @ApiModelProperty("社保缴纳费用")
    private BigDecimal socialPay;

    @ExcelProperty("基础工资")
    @ApiModelProperty("基础工资")
    private BigDecimal baseSalary;

    @ExcelProperty("加班费")
    @ApiModelProperty("加班费")
    private BigDecimal overtimeSalary;

    @ApiModelProperty("备注")
    private String remark;

    @ExcelProperty("生活补贴")
    @ApiModelProperty("生活补贴")
    private BigDecimal subsidy;

    @ExcelProperty("奖金")
    @ApiModelProperty("奖金")
    private BigDecimal bonus;

    @ApiModelProperty("月份")
    private String month;

    @ExcelProperty("最终工资")
    @ApiModelProperty("最终工资")
    private BigDecimal totalSalary;

}
