package com.qiujie.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 加班导入 Excel 行 DTO。
 *
 * @author qiujie
 */
@Data
public class OvertimeImportRow {

    private Integer rowNum;

    @ExcelProperty("员工id")
    private Integer staffId;

    @ExcelProperty("上午上班时间")
    private Timestamp morStartTime;

    @ExcelProperty("上午下班时间")
    private Timestamp morEndTime;

    @ExcelProperty("下午上班时间")
    private Timestamp aftStartTime;

    @ExcelProperty("下午下班时间")
    private Timestamp aftEndTime;

    @ExcelProperty("加班日期")
    private Date overtimeDate;
}
