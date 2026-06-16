package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
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
 * @since 2022-03-23
 */
@Getter
@Setter
@TableName("soc_city")
@Schema(description = "City对象 - 参保城市")
public class City implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("城市")
    @Schema(description = "参保城市")
    @TableField("name")
    private String name;

    @Schema(description = "职工上年度平均月工资")
    @TableField("average_salary")
    private BigDecimal averageSalary;

    @Schema(description = "职工上年度最低月工资")
    @TableField("lower_salary")
    private BigDecimal lowerSalary;

    @ExcelProperty("职工社保缴纳基数上限")
    @Schema(description = "职工社保缴纳基数上限")
    @TableField("soc_upper_limit")
    private BigDecimal socUpperLimit;

    @ExcelProperty("职工社保缴纳基数下限")
    @Schema(description = "职工社保缴纳基数下限")
    @TableField("soc_lower_limit")
    private BigDecimal socLowerLimit;

    @ExcelProperty("公积金缴纳基数上限")
    @Schema(description = "公积金缴纳基数上限")
    @TableField("hou_upper_limit")
    private BigDecimal houUpperLimit;

    @ExcelProperty("公积金缴纳基数下限")
    @Schema(description = "公积金缴纳基数下限")
    @TableField("hou_lower_limit")
    private BigDecimal houLowerLimit;

    @ExcelProperty("养老保险个人缴费比例")
    @Schema(description = "养老保险个人缴费比例")
    @TableField("per_pension_rate")
    private BigDecimal perPensionRate;

    @ExcelProperty("养老保险企业缴费比例")
    @Schema(description = "养老保险企业缴费比例")
    @TableField("com_pension_rate")
    private BigDecimal comPensionRate;

    @ExcelProperty("医疗保险个人缴费比例")
    @Schema(description = "医疗保险个人缴费比例")
    @TableField("per_medical_rate")
    private BigDecimal perMedicalRate;

    @ExcelProperty("医疗保险企业缴费比例")
    @Schema(description = "医疗保险企业缴费比例")
    @TableField("com_medical_rate")
    private BigDecimal comMedicalRate;

    @ExcelProperty("失业保险个人缴费比例")
    @Schema(description = "失业保险个人缴费比例")
    @TableField("per_unemployment_rate")
    private BigDecimal perUnemploymentRate;

    @ExcelProperty("失业保险企业缴费比例")
    @Schema(description = "失业保险企业缴费比例")
    @TableField("com_unemployment_rate")
    private BigDecimal comUnemploymentRate;

    @ExcelProperty("生育保险企业缴费比例")
    @Schema(description = "生育保险企业缴费比例")
    @TableField("com_maternity_rate")
    private BigDecimal comMaternityRate;

    @ExcelProperty("备注")
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
