package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.alibaba.excel.annotation.ExcelProperty;
import com.qiujie.enums.AttendanceStatusEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.sql.Timestamp;
import java.sql.Date;

/**
 * <p>
 *
 * </p>
 *
 * @author qiujie
 * @since 2022-03-29
 */
@Data
@Accessors(chain = true)
@TableName("att_attendance")
@ApiModel(value = "Attendance对象", description = "")
public class Attendance implements Serializable {

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

    @ExcelProperty("考勤日期")
    @ApiModelProperty("考勤日期")
    @TableField("attendance_date")
    private Date attendanceDate;

    @ApiModelProperty("0正常，1迟到，2早退，3旷工，4休假")
    @TableField("status")
    private AttendanceStatusEnum status;

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
