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
 * @since 2022-03-24
 */
@Getter
@Setter
@TableName("soc_insurance")
@Schema(description = "Insurance对象 - 员工社保")
public class Insurance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "城市id")
    @TableField("city_id")
    private Integer cityId;

    @Schema(description = "员工id")
    @TableField("staff_id")
    private Integer staffId;

    @Schema(description = "公积金基数")
    @TableField("house_base")
    private BigDecimal houseBase;

    @Schema(description = "公积金个人缴纳比例")
    @TableField("per_house_rate")
    private BigDecimal perHouseRate;

    @Schema(description = "公积金个人缴纳费用")
    @TableField("per_house_pay")
    private BigDecimal perHousePay;

    @Schema(description = "公积金企业缴纳比例")
    @TableField("com_house_rate")
    private BigDecimal comHouseRate;

    @Schema(description = "公积金企业缴纳费用")
    @TableField("com_house_pay")
    private BigDecimal comHousePay;

    @Schema(description = "公积金备注")
    @TableField("house_remark")
    private String houseRemark;

    @Schema(description = "社保基数")
    @TableField("social_base")
    private BigDecimal socialBase;

    @Schema(description = "社保企业缴纳费用")
    @TableField("com_social_pay")
    private BigDecimal comSocialPay;

    @Schema(description = "社保个人缴纳费用")
    @TableField("per_social_pay")
    private BigDecimal perSocialPay;

    @Schema(description = "工伤保险企业缴纳比例")
    @TableField("com_injury_rate")
    private BigDecimal comInjuryRate;

    @Schema(description = "社保备注")
    @TableField("social_remark")
    private String socialRemark;

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
