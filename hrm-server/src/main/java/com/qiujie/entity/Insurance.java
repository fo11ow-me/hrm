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
 * @since 2022-03-24
 */
@Getter
@Setter
@TableName("soc_insurance")
@ApiModel(value = "Insurance对象", description = "员工社保")
public class Insurance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("城市id")
    @TableField("city_id")
    private Integer cityId;

    @ApiModelProperty("员工id")
    @TableField("staff_id")
    private Integer staffId;

    @ApiModelProperty("公积金基数")
    @TableField("house_base")
    private BigDecimal houseBase;

    @ApiModelProperty("公积金个人缴纳比例")
    @TableField("per_house_rate")
    private BigDecimal perHouseRate;

    @ApiModelProperty("公积金个人缴纳费用")
    @TableField("per_house_pay")
    private BigDecimal perHousePay;

    @ApiModelProperty("公积金企业缴纳比例")
    @TableField("com_house_rate")
    private BigDecimal comHouseRate;

    @ApiModelProperty("公积金企业缴纳费用")
    @TableField("com_house_pay")
    private BigDecimal comHousePay;

    @ApiModelProperty("公积金备注")
    @TableField("house_remark")
    private String houseRemark;

    @ApiModelProperty("社保基数")
    @TableField("social_base")
    private BigDecimal socialBase;

    @ApiModelProperty("社保企业缴纳费用")
    @TableField("com_social_pay")
    private BigDecimal comSocialPay;

    @ApiModelProperty("社保个人缴纳费用")
    @TableField("per_social_pay")
    private BigDecimal perSocialPay;

    @ApiModelProperty("工伤保险企业缴纳比例")
    @TableField("com_injury_rate")
    private BigDecimal comInjuryRate;

    @ApiModelProperty("社保备注")
    @TableField("social_remark")
    private String socialRemark;

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
