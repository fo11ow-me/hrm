package com.qiujie.util;

import com.alibaba.excel.EasyExcel;
import com.qiujie.dto.AttendanceImportRow;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 测试用 Excel 文件生成工具。
 * 使用 EasyExcel 程序化生成考勤导入测试文件，支持：
 * - 纯有效数据
 * - 混合错误数据（空 staffId、空日期、不存在的员工）
 * - 大量数据（可指定行数）
 *
 * @author qiujie
 */
public class TestExcelUtil {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /**
     * 生成包含有效考勤数据的 Excel 文件，日期从 baseDate 开始循环 28 天周期
     *
     * @param filePath 输出文件路径
     * @param rowCount 数据行数
     * @param baseDate 起始日期，格式 yyyyMMdd
     */
    public static File createAttendanceImportExcel(String filePath, int rowCount, String baseDate) {
        List<AttendanceImportRow> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            AttendanceImportRow row = new AttendanceImportRow();
            row.setStaffId(1 + (i % 3));
            try {
                Date base = DATE_FORMAT.parse(baseDate);
                long offset = (i % 28) * 24L * 3600 * 1000;
                row.setAttendanceDate(new Date(base.getTime() + offset));
            } catch (ParseException e) {
                row.setAttendanceDate(new Date());
            }
            row.setMorStartTime(parseTime("09:00:00"));
            row.setMorEndTime(parseTime("12:00:00"));
            row.setAftStartTime(parseTime("13:00:00"));
            row.setAftEndTime(parseTime("18:00:00"));
            row.setRowNum(i + 3);
            rows.add(row);
        }
        return writeAndReturn(filePath, rows);
    }

    /**
     * 生成包含混合错误数据的 Excel 文件
     * 行1：正常数据
     * 行2：空 staffId
     * 行3：空日期
     * 行4：不存在的员工（staffId=999）
     * 行5-7：正常数据
     * 行8：部门不存在（staff 的 deptId 在 DB 中不存在）
     */
    public static File createAttendanceImportExcelWithErrors(String filePath) {
        List<AttendanceImportRow> rows = new ArrayList<>();

        AttendanceImportRow row1 = new AttendanceImportRow();
        row1.setStaffId(1);
        row1.setAttendanceDate(parseDate("20240102"));
        row1.setMorStartTime(parseTime("09:00:00"));
        row1.setMorEndTime(parseTime("12:00:00"));
        row1.setAftStartTime(parseTime("13:00:00"));
        row1.setAftEndTime(parseTime("18:00:00"));
        row1.setRowNum(3);
        rows.add(row1);

        AttendanceImportRow row2 = new AttendanceImportRow();
        row2.setStaffId(null);
        row2.setAttendanceDate(parseDate("20240102"));
        row2.setRowNum(4);
        rows.add(row2);

        AttendanceImportRow row3 = new AttendanceImportRow();
        row3.setStaffId(1);
        row3.setAttendanceDate(null);
        row3.setRowNum(5);
        rows.add(row3);

        AttendanceImportRow row4 = new AttendanceImportRow();
        row4.setStaffId(999);
        row4.setAttendanceDate(parseDate("20240102"));
        row4.setRowNum(6);
        rows.add(row4);

        for (int i = 0; i < 4; i++) {
            AttendanceImportRow row = new AttendanceImportRow();
            row.setStaffId(2);
            try {
                Date base = DATE_FORMAT.parse("20240102");
                long offset = (i + 1) * 24L * 3600 * 1000;
                row.setAttendanceDate(new Date(base.getTime() + offset));
            } catch (ParseException e) {
                row.setAttendanceDate(new Date());
            }
            row.setMorStartTime(parseTime("09:00:00"));
            row.setMorEndTime(parseTime("12:00:00"));
            row.setAftStartTime(parseTime("13:00:00"));
            row.setAftEndTime(parseTime("18:00:00"));
            row.setRowNum(7 + i);
            rows.add(row);
        }

        return writeAndReturn(filePath, rows);
    }

    /**
     * 生成包含不同考勤状态的数据：正常、迟到、早退、旷工
     */
    public static File createAttendanceImportExcelWithStatusVariations(String filePath) {
        List<AttendanceImportRow> rows = new ArrayList<>();

        // 正常：所有时间在部门范围内
        AttendanceImportRow normal = new AttendanceImportRow();
        normal.setStaffId(1);
        normal.setAttendanceDate(parseDate("20240102"));
        normal.setMorStartTime(parseTime("08:55:00"));
        normal.setMorEndTime(parseTime("12:05:00"));
        normal.setAftStartTime(parseTime("12:55:00"));
        normal.setAftEndTime(parseTime("18:05:00"));
        normal.setRowNum(3);
        rows.add(normal);

        // 迟到：上午打卡时间晚于部门上班时间(09:00)
        AttendanceImportRow late = new AttendanceImportRow();
        late.setStaffId(1);
        late.setAttendanceDate(parseDate("20240103"));
        late.setMorStartTime(parseTime("09:30:00"));
        late.setMorEndTime(parseTime("12:00:00"));
        late.setAftStartTime(parseTime("13:00:00"));
        late.setAftEndTime(parseTime("18:00:00"));
        late.setRowNum(4);
        rows.add(late);

        // 早退：下午下班时间早于部门下班时间(18:00)
        AttendanceImportRow leaveEarly = new AttendanceImportRow();
        leaveEarly.setStaffId(1);
        leaveEarly.setAttendanceDate(parseDate("20240104"));
        leaveEarly.setMorStartTime(parseTime("09:00:00"));
        leaveEarly.setMorEndTime(parseTime("12:00:00"));
        leaveEarly.setAftStartTime(parseTime("13:00:00"));
        leaveEarly.setAftEndTime(parseTime("17:00:00"));
        leaveEarly.setRowNum(5);
        rows.add(leaveEarly);

        // 旷工：缺少上午上下班时间
        AttendanceImportRow absenteeism = new AttendanceImportRow();
        absenteeism.setStaffId(2);
        absenteeism.setAttendanceDate(parseDate("20240102"));
        absenteeism.setMorStartTime(null);
        absenteeism.setMorEndTime(null);
        absenteeism.setAftStartTime(parseTime("13:00:00"));
        absenteeism.setAftEndTime(parseTime("18:00:00"));
        absenteeism.setRowNum(6);
        rows.add(absenteeism);

        return writeAndReturn(filePath, rows);
    }

    public static File createEmptyAttendanceImportExcel(String filePath) {
        return writeAndReturn(filePath, new ArrayList<>());
    }

    private static File writeAndReturn(String filePath, List<AttendanceImportRow> rows) {
        File file = new File(filePath);
        EasyExcel.write(file, AttendanceImportRow.class).sheet("data").doWrite(rows);
        return file;
    }

    private static Date parseTime(String timeStr) {
        try {
            return TIME_FORMAT.parse(timeStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private static Date parseDate(String dateStr) {
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
}
