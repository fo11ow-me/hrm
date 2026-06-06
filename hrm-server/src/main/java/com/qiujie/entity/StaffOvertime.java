package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.alibaba.excel.annotation.ExcelProperty;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Date;

/**
 * <p>
 * 员工考勤表
 * </p>
 *
 * @author qiujie
 * @since 2024-03-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("att_staff_overtime")
@ApiModel(value = "StaffOvertime对象", description = "员工加班表")
public class StaffOvertime implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("员工id")
    @ApiModelProperty("员工id")
    @TableField("staff_id")
    private Integer staffId;

    @ExcelProperty("上午上班时间")
    @ApiModelProperty("上午上班时间")
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @TableField("mor_start_time")
    private Timestamp morStartTime;

    @ExcelProperty("上午下班时间")
    @ApiModelProperty("上午下班时间")
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @TableField("mor_end_time")
    private Timestamp morEndTime;

    @ExcelProperty("下午上班时间")
    @ApiModelProperty("下午上班时间")
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @TableField("aft_start_time")
    private Timestamp aftStartTime;

    @ExcelProperty("下午下班时间")
    @ApiModelProperty("下午下班时间")
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @TableField("aft_end_time")
    private Timestamp aftEndTime;

    @ExcelProperty("加班日期")
    @ApiModelProperty("加班日期")
    @TableField("overtime_date")
    private Date overtimeDate;

    @ApiModelProperty("加班时长")
    @TableField("total_overtime")
    private BigDecimal totalOvertime;

    @ApiModelProperty("加班工资")
    @TableField("overtime_salary")
    private BigDecimal overtimeSalary;

    @ApiModelProperty("加班类型")
    @TableField("type_num")
    private OvertimeEnum typeNum;

    @ApiModelProperty("0正常，1加班，2调休")
    @TableField("status")
    private OvertimeStatusEnum status;

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
