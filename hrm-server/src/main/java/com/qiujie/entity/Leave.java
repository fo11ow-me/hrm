package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qiujie.enums.LeaveEnum;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * <p>
 * 请假表
 * </p>
 *
 * @author qiujie
 * @since 2022-03-27
 */
@Getter
@Setter
@TableName("att_leave")
@ApiModel(value = "Leave对象", description = "请假表")
public class Leave implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("休假类型")
    @TableField("type_num")
    private LeaveEnum typeNum;

    @ApiModelProperty("部门id")
    @TableField("dept_id")
    private Integer deptId;

    @ApiModelProperty("休假天数")
    @TableField("days")
    private Integer days;

    @ApiModelProperty("0禁用，1正常，默认1")
    @TableField("status")
    private Integer status;

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
