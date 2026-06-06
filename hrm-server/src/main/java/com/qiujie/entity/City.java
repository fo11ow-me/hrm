package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * <p>
 *
 * </p>
 *
 * @author qiujie
 * @since 2022-03-23
 */
@Getter
@Setter
@TableName("soc_city")
@ApiModel(value = "City对象", description = "参保城市")
public class City implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("城市")
    @ApiModelProperty("参保城市")
    @TableField("name")
    private String name;

    @ApiModelProperty("职工上年度平均月工资")
    @TableField("average_salary")
    private BigDecimal averageSalary;

    @ApiModelProperty("职工上年度最低月工资")
    @TableField("lower_salary")
    private BigDecimal lowerSalary;

    @ExcelProperty("职工社保缴纳基数上限")
    @ApiModelProperty("职工社保缴纳基数上限")
    @TableField("soc_upper_limit")
    private BigDecimal socUpperLimit;

    @ExcelProperty("职工社保缴纳基数下限")
    @ApiModelProperty("职工社保缴纳基数下限")
    @TableField("soc_lower_limit")
    private BigDecimal socLowerLimit;

    @ExcelProperty("公积金缴纳基数上限")
    @ApiModelProperty("公积金缴纳基数上限")
    @TableField("hou_upper_limit")
    private BigDecimal houUpperLimit;

    @ExcelProperty("公积金缴纳基数下限")
    @ApiModelProperty("公积金缴纳基数下限")
    @TableField("hou_lower_limit")
    private BigDecimal houLowerLimit;

    @ExcelProperty("养老保险个人缴费比例")
    @ApiModelProperty("养老保险个人缴费比例")
    @TableField("per_pension_rate")
    private BigDecimal perPensionRate;

    @ExcelProperty("养老保险企业缴费比例")
    @ApiModelProperty("养老保险企业缴费比例")
    @TableField("com_pension_rate")
    private BigDecimal comPensionRate;

    @ExcelProperty("医疗保险个人缴费比例")
    @ApiModelProperty("医疗保险个人缴费比例")
    @TableField("per_medical_rate")
    private BigDecimal perMedicalRate;

    @ExcelProperty("医疗保险企业缴费比例")
    @ApiModelProperty("医疗保险企业缴费比例")
    @TableField("com_medical_rate")
    private BigDecimal comMedicalRate;

    @ExcelProperty("失业保险个人缴费比例")
    @ApiModelProperty("失业保险个人缴费比例")
    @TableField("per_unemployment_rate")
    private BigDecimal perUnemploymentRate;

    @ExcelProperty("失业保险企业缴费比例")
    @ApiModelProperty("失业保险企业缴费比例")
    @TableField("com_unemployment_rate")
    private BigDecimal comUnemploymentRate;

    @ExcelProperty("生育保险企业缴费比例")
    @ApiModelProperty("生育保险企业缴费比例")
    @TableField("com_maternity_rate")
    private BigDecimal comMaternityRate;

    @ExcelProperty("备注")
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
