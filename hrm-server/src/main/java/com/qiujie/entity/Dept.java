package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * <p>
 * 
 * </p>
 *
 * @author qiujie
 * @since 2022-03-07
 */

@Getter
@Setter
@TableName("sys_dept")
@ApiModel(value = "Dept对象", description = "部门")
public class Dept implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("部门id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("部门编码")
    @TableField("code")
    private String code;

    @ApiModelProperty("部门名称")
    @TableField("name")
    private String name;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @ApiModelProperty("上午上班时间")
    @TableField("mor_start_time")
    private Timestamp morStartTime;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @ApiModelProperty("上午下班时间")
    @TableField("mor_end_time")
    private Timestamp morEndTime;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @ApiModelProperty("下午上班时间")
    @TableField("aft_start_time")
    private Timestamp aftStartTime;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @ApiModelProperty("下午下班时间")
    @TableField("aft_end_time")
    private Timestamp aftEndTime;

    @ApiModelProperty("员工的总工作时长")
    @TableField("total_work_time")
    private BigDecimal totalWorkTime;

    @ApiModelProperty("部门备注")
    @TableField("remark")
    private String remark;

    @ApiModelProperty("父级部门id，0根部门")
    @TableField("parent_id")
    private Integer parentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @ApiModelProperty("逻辑删除，0未删除，1删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;

    @ApiModelProperty("子部门")
    @TableField(exist = false)
    private List<Dept> children;

}
