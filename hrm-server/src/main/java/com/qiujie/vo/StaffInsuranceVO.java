package com.qiujie.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author qiujie
 * @Date 2022/3/23
 * @Version 1.0
 */

@Data
public class StaffInsuranceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("员工id")
    private Integer staffId;

    @ApiModelProperty("城市id")
    private Integer cityId;

    @ApiModelProperty("社保id")
    private Integer insuranceId;

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

    @ExcelProperty("公积金基数")
    @ApiModelProperty("公积金基数")
    private BigDecimal houseBase;

    @ExcelProperty("公积金个人缴纳比例")
    @ApiModelProperty("公积金个人缴纳比例")
    private BigDecimal perHouseRate;

    @ExcelProperty("公积金个人缴纳费用")
    @ApiModelProperty("公积金个人缴纳费用")
    private BigDecimal perHousePay;

    @ExcelProperty("公积金企业缴纳比例")
    @ApiModelProperty("公积金企业缴纳比例")
    private BigDecimal comHouseRate;

    @ExcelProperty("公积金企业缴纳费用")
    @ApiModelProperty("公积金企业缴纳费用")
    private BigDecimal comHousePay;

    @ExcelProperty("公积金备注")
    @ApiModelProperty("公积金备注")
    private String houseRemark;

    @ExcelProperty("社保基数")
    @ApiModelProperty("社保基数")
    private BigDecimal socialBase;

    @ExcelProperty("社保企业缴纳费用")
    @ApiModelProperty("社保企业缴纳费用")
    private BigDecimal comSocialPay;

    @ExcelProperty("社保个人缴纳费用")
    @ApiModelProperty("社保个人缴纳费用")
    private BigDecimal perSocialPay;

    @ExcelProperty("工伤保险企业缴纳比例")
    @ApiModelProperty("工伤保险企业缴纳比例")
    private BigDecimal comInjuryRate;

    @ExcelProperty("社保备注")
    @ApiModelProperty("社保备注")
    private String socialRemark;


}
