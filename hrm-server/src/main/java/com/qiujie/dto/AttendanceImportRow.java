package com.qiujie.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class AttendanceImportRow {

    private Integer rowNum;

    @ExcelProperty("员工id")
    private Integer staffId;

    @ExcelProperty("上午上班时间")
    private Date morStartTime;

    @ExcelProperty("上午下班时间")
    private Date morEndTime;

    @ExcelProperty("下午上班时间")
    private Date aftStartTime;

    @ExcelProperty("下午下班时间")
    private Date aftEndTime;

    @ExcelProperty("考勤日期")
    private Date attendanceDate;
}
