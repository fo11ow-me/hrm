package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;


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
@Schema(description = "Dept对象 - 部门")
public class Dept implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "部门编码")
    @TableField("code")
    private String code;

    @Schema(description = "部门名称")
    @TableField("name")
    private String name;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @Schema(description = "上午上班时间")
    @TableField("mor_start_time")
    private Timestamp morStartTime;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @Schema(description = "上午下班时间")
    @TableField("mor_end_time")
    private Timestamp morEndTime;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @Schema(description = "下午上班时间")
    @TableField("aft_start_time")
    private Timestamp aftStartTime;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @Schema(description = "下午下班时间")
    @TableField("aft_end_time")
    private Timestamp aftEndTime;

    @Schema(description = "员工的总工作时长")
    @TableField("total_work_time")
    private BigDecimal totalWorkTime;

    @Schema(description = "部门备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "父级部门id，0根部门")
    @TableField("parent_id")
    private Integer parentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @Schema(description = "逻辑删除，0未删除，1删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;

    @Schema(description = "子部门")
    @TableField(exist = false)
    private List<Dept> children;

}
