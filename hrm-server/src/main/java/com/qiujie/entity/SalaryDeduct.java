package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qiujie.enums.DeductEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * <p>
 * 工资扣除表
 * </p>
 *
 * @author qiujie
 * @since 2022-03-27
 */
@Getter
@Setter
@TableName("sal_salary_deduct")
@ApiModel(value = "SalaryDeduct对象", description = "工资扣除表")
public class SalaryDeduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("部门id")
    @TableField("dept_id")
    private Integer deptId;

    @ApiModelProperty("扣款类型，0迟到，1早退，2旷工，3休假")
    @TableField("type_num")
    private DeductEnum typeNum;

    @ApiModelProperty("每次扣款金额")
    @TableField("deduct")
    private Integer deduct;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("修改时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @ApiModelProperty("0未删除，1已删除，默认为0")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;


}
