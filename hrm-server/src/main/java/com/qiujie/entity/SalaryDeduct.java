package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import com.qiujie.enums.DeductEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "SalaryDeduct对象 - 工资扣除表")
public class SalaryDeduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "部门id")
    @TableField("dept_id")
    private Integer deptId;

    @Schema(description = "扣款类型，0迟到，1早退，2旷工，3休假")
    @TableField("type_num")
    private DeductEnum typeNum;

    @Schema(description = "每次扣款金额")
    @TableField("deduct")
    private Integer deduct;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "修改时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @Schema(description = "0未删除，1已删除，默认为0")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;


}
