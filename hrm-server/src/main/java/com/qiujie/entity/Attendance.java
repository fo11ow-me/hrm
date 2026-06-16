package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import com.qiujie.enums.AttendanceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.Accessors;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Date;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Attendance对象 - ")
public class Attendance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("员工id")
    @Schema(description = "员工id")
    @TableField("staff_id")
    private Integer staffId;

    @ExcelProperty("上午上班时间")
    @Schema(description = "上午上班时间")
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @TableField("mor_start_time")
    private Timestamp morStartTime;

    @ExcelProperty("上午下班时间")
    @Schema(description = "上午下班时间")
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @TableField("mor_end_time")
    private Timestamp morEndTime;

    @ExcelProperty("下午上班时间")
    @Schema(description = "下午上班时间")
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @TableField("aft_start_time")
    private Timestamp aftStartTime;

    @ExcelProperty("下午下班时间")
    @Schema(description = "下午下班时间")
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    @TableField("aft_end_time")
    private Timestamp aftEndTime;

    @ExcelProperty("考勤日期")
    @Schema(description = "考勤日期")
    @TableField("attendance_date")
    private Date attendanceDate;

    @Schema(description = "0正常，1迟到，2早退，3旷工，4休假")
    @TableField("status")
    private AttendanceStatusEnum status;

    @TableField("remark")
    private String remark;

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
