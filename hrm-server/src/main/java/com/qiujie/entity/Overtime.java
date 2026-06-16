package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import com.qiujie.enums.OvertimeEnum;
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
 * 加班表
 * </p>
 *
 * @author qiujie
 * @since 2022-03-28
 */
@Getter
@Setter
@TableName("att_overtime")
@Schema(description = "Overtime对象 - 加班表")
public class Overtime implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "工资倍数，如按照小时计算，就是员工平均小时工资乘以倍数")
    @TableField("salary_multiple")
    private BigDecimal salaryMultiple;

    @Schema(description = "加班奖金")
    @TableField("bonus")
    private BigDecimal bonus;

    @Schema(description = "加班类型")
    @TableField("type_num")
    private OvertimeEnum typeNum;

    @Schema(description = "部门id")
    @TableField("dept_id")
    private Integer deptId;

    @Schema(description = "0小时，1天，默认0，计数加班工资的类型")
    @TableField("count_type")
    private Integer countType;

    @TableField("remark")
    private String remark;

    @Schema(description = "0不调休，1调休，默认0")
    @TableField("is_time_off")
    private Integer timeOffFlag;


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
