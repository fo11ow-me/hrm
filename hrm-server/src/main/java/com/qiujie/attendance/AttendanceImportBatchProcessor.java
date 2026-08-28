package com.qiujie.attendance;

import com.qiujie.dto.AttendanceImportRow;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.Dept;
import com.qiujie.entity.FileTaskError;
import com.qiujie.entity.Staff;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.DeptMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.DatetimeUtil;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cn.hutool.core.date.DateUtil;

/**
 * 考勤导入批处理领域服务。
 * <p>
 * 统一同步导入和异步文件任务使用的预加载、校验、状态判定与批量写入规则。
 * 不负责文件读取、任务状态或错误记录持久化。
 * </p>
 * <p>
 * 迟到/早退/旷工判定规则同时供 {@code AttendanceService} 使用（见
 * {@link #isLate} / {@link #isLeaveEarly} / {@link #isAbsenteeism}），
 * 避免两处逻辑漂移。
 * </p>
 */
public class AttendanceImportBatchProcessor {

    private static final int DB_BATCH_SIZE = 200;

    private final AttendanceMapper attendanceMapper;
    private final DeptMapper deptMapper;
    private final StaffMapper staffMapper;
    private final TransactionTemplate transactionTemplate;

    public AttendanceImportBatchProcessor(AttendanceMapper attendanceMapper,
                                          DeptMapper deptMapper,
                                          StaffMapper staffMapper,
                                          TransactionTemplate transactionTemplate) {
        this.attendanceMapper = attendanceMapper;
        this.deptMapper = deptMapper;
        this.staffMapper = staffMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public Result process(List<AttendanceImportRow> rows, Long taskId,
                          Consumer<FileTaskError> errorCollector) {
        Result result = new Result();
        if (rows == null || rows.isEmpty()) {
            return result;
        }

        Set<Integer> staffIds = rows.stream()
                .map(AttendanceImportRow::getStaffId)
                .filter(item -> item != null)
                .collect(Collectors.toSet());
        Map<Integer, Staff> staffMap = staffIds.isEmpty()
                ? new HashMap<>()
                : staffMapper.selectBatchIds(staffIds).stream()
                .collect(Collectors.toMap(Staff::getId, item -> item));
        Set<Integer> deptIds = staffMap.values().stream()
                .map(Staff::getDeptId)
                .filter(item -> item != null)
                .collect(Collectors.toSet());
        Map<Integer, Dept> deptMap = deptIds.isEmpty()
                ? new HashMap<>()
                : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Dept::getId, item -> item));
        Set<Date> attendanceDates = rows.stream()
                .map(AttendanceImportRow::getAttendanceDate)
                .filter(item -> item != null)
                .map(this::toSqlDate)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, Attendance> existingMap = new HashMap<>();
        if (!staffIds.isEmpty() && !attendanceDates.isEmpty()) {
            existingMap = attendanceMapper.queryByStaffIdsAndDates(staffIds, attendanceDates).stream()
                    .collect(Collectors.toMap(this::buildAttendanceKey, item -> item, (left, right) -> left));
        }

        List<Attendance> saveList = new ArrayList<>();
        for (AttendanceImportRow row : rows) {
            String rowData = buildRowData(row);
            int rowNum = row.getRowNum() == null ? 0 : row.getRowNum();
            if (row.getStaffId() == null) {
                errorCollector.accept(buildError(taskId, rowNum, rowData, "员工id不能为空"));
                continue;
            }
            if (row.getAttendanceDate() == null) {
                errorCollector.accept(buildError(taskId, rowNum, rowData, "考勤日期不能为空"));
                continue;
            }
            Staff staff = staffMap.get(row.getStaffId());
            if (staff == null) {
                errorCollector.accept(buildError(taskId, rowNum, rowData, "员工不存在"));
                continue;
            }
            Dept dept = deptMap.get(staff.getDeptId());
            if (dept == null) {
                errorCollector.accept(buildError(taskId, rowNum, rowData, "员工部门不存在"));
                continue;
            }

            Attendance attendance = convertAttendance(row);
            if (DateUtil.isWeekend(attendance.getAttendanceDate())) {
                result.successCount++;
                continue;
            }
            Attendance existing = existingMap.get(buildAttendanceKey(attendance));
            if (existing != null && (existing.getStatus() == AttendanceStatusEnum.LEAVE
                    || existing.getStatus() == AttendanceStatusEnum.TIME_OFF)) {
                result.successCount++;
                continue;
            }
            if (existing != null) {
                attendance.setId(existing.getId());
            }
            if (isAbsenteeism(attendance, dept)) {
                attendance.setStatus(AttendanceStatusEnum.ABSENTEEISM);
            } else if (isLate(attendance, dept)) {
                attendance.setStatus(AttendanceStatusEnum.LATE);
            } else if (isLeaveEarly(attendance, dept)) {
                attendance.setStatus(AttendanceStatusEnum.LEAVE_EARLY);
            } else {
                attendance.setStatus(AttendanceStatusEnum.NORMAL);
            }
            saveList.add(attendance);
        }

        if (!saveList.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> saveOrUpdate(saveList));
            result.successCount += saveList.size();
        }
        result.totalCount = rows.size();
        result.processedCount = rows.size();
        result.failCount = result.totalCount - result.successCount;
        return result;
    }

    private void saveOrUpdate(List<Attendance> attendances) {
        for (int from = 0; from < attendances.size(); from += DB_BATCH_SIZE) {
            int to = Math.min(from + DB_BATCH_SIZE, attendances.size());
            // 使用实体 id 区分更新和新增，避免重复导入产生重复记录。
            for (Attendance attendance : attendances.subList(from, to)) {
                if (attendance.getId() == null) {
                    attendanceMapper.insert(attendance);
                } else {
                    attendanceMapper.updateById(attendance);
                }
            }
        }
    }

    public boolean isLate(Attendance attendance, Dept dept) {
        if (attendance.getMorStartTime() == null || attendance.getAftStartTime() == null) {
            return false;
        }
        return DateUtil.compare(attendance.getMorStartTime(), dept.getMorStartTime(), "HH:mm:ss") > 0
                || DateUtil.compare(attendance.getAftStartTime(), dept.getAftStartTime(), "HH:mm:ss") > 0;
    }

    public boolean isLeaveEarly(Attendance attendance, Dept dept) {
        if (attendance.getMorEndTime() == null || attendance.getAftEndTime() == null) {
            return false;
        }
        return DateUtil.compare(attendance.getMorEndTime(), dept.getMorEndTime(), "HH:mm:ss") < 0
                || DateUtil.compare(attendance.getAftEndTime(), dept.getAftEndTime(), "HH:mm:ss") < 0;
    }

    public boolean isAbsenteeism(Attendance attendance, Dept dept) {
        if (attendance.getMorStartTime() == null || attendance.getMorEndTime() == null
                || attendance.getAftStartTime() == null || attendance.getAftEndTime() == null) {
            return true;
        }
        return isLate(attendance, dept) && isLeaveEarly(attendance, dept);
    }

    private Attendance convertAttendance(AttendanceImportRow row) {
        Attendance attendance = new Attendance();
        attendance.setStaffId(row.getStaffId());
        attendance.setMorStartTime(toSqlTimestamp(row.getMorStartTime()));
        attendance.setMorEndTime(toSqlTimestamp(row.getMorEndTime()));
        attendance.setAftStartTime(toSqlTimestamp(row.getAftStartTime()));
        attendance.setAftEndTime(toSqlTimestamp(row.getAftEndTime()));
        attendance.setAttendanceDate(toSqlDate(row.getAttendanceDate()));
        return attendance;
    }

    private FileTaskError buildError(Long taskId, int rowNum, String rawData, String message) {
        FileTaskError error = new FileTaskError();
        error.setTaskId(taskId);
        error.setRowNum(rowNum);
        error.setRawData(rawData);
        error.setErrorMessage(message);
        return error;
    }

    private String buildRowData(AttendanceImportRow row) {
        String date = row.getAttendanceDate() == null ? "" : DateUtil.formatDate(row.getAttendanceDate());
        return "staffId=" + row.getStaffId() + ", attendanceDate=" + date;
    }

    private String buildAttendanceKey(Attendance attendance) {
        return attendance.getStaffId() + "_" + attendance.getAttendanceDate();
    }

    private Timestamp toSqlTimestamp(java.util.Date date) {
        if (date == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return Timestamp.valueOf(LocalDateTime.of(1970, 1, 1,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
    }

    private Date toSqlDate(java.util.Date date) {
        return date == null ? null : new Date(date.getTime());
    }

    /** 处理结果统计。 */
    public static final class Result {
        public int totalCount;
        public int processedCount;
        public int successCount;
        public int failCount;
    }
}
