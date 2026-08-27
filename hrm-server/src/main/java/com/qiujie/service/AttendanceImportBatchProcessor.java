package com.qiujie.service;

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
 */
final class AttendanceImportBatchProcessor {

    private static final int DB_BATCH_SIZE = 200;

    private final AttendanceMapper attendanceMapper;
    private final DeptMapper deptMapper;
    private final StaffMapper staffMapper;
    private final TransactionTemplate transactionTemplate;

    AttendanceImportBatchProcessor(AttendanceMapper attendanceMapper,
                                   DeptMapper deptMapper,
                                   StaffMapper staffMapper,
                                   TransactionTemplate transactionTemplate) {
        this.attendanceMapper = attendanceMapper;
        this.deptMapper = deptMapper;
        this.staffMapper = staffMapper;
        this.transactionTemplate = transactionTemplate;
    }

    Result process(List<AttendanceImportRow> rows, Long taskId,
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

    boolean isLate(Attendance attendance, Dept dept) {
        if (attendance.getMorStartTime() == null || attendance.getAftStartTime() == null) {
            return false;
        }
        return DateUtil.compare(attendance.getMorStartTime(), dept.getMorStartTime(), "HH:mm:ss") > 0
                || DateUtil.compare(attendance.getAftStartTime(), dept.getAftStartTime(), "HH:mm:ss") > 0;
    }

    boolean isLeaveEarly(Attendance attendance, Dept dept) {
        if (attendance.getMorEndTime() == null || attendance.getAftEndTime() == null) {
            return false;
        }
        return DateUtil.compare(attendance.getMorEndTime(), dept.getMorEndTime(), "HH:mm:ss") < 0
                || DateUtil.compare(attendance.getAftEndTime(), dept.getAftEndTime(), "HH:mm:ss") < 0;
    }

    boolean isAbsenteeism(Attendance attendance, Dept dept) {
        if (attendance.getMorStartTime() == null || attendance.getMorEndTime() == null
                || attendance.getAftStartTime() == null || attendance.getAftEndTime() == null) {
            return true;
        }
        return isLate(attendance, dept) && isLeaveEarly(attendance, dept);
    }

    private Attendance convertAttendance(AttendanceImportRow row) {
        return new Attendance()
                .setStaffId(row.getStaffId())
                .setMorStartTime(toSqlTimestamp(row.getMorStartTime()))
                .setMorEndTime(toSqlTimestamp(row.getMorEndTime()))
                .setAftStartTime(toSqlTimestamp(row.getAftStartTime()))
                .setAftEndTime(toSqlTimestamp(row.getAftEndTime()))
                .setAttendanceDate(toSqlDate(row.getAttendanceDate()));
    }

    private FileTaskError buildError(Long taskId, int rowNum, String rawData, String message) {
        return new FileTaskError().setTaskId(taskId).setRowNum(rowNum)
                .setRawData(rawData).setErrorMessage(message);
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

    static final class Result {
        int totalCount;
        int processedCount;
        int successCount;
        int failCount;
    }
}
