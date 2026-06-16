package com.qiujie.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskTypeEnum;
import com.qiujie.dto.AttendanceImportRow;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.Dept;
import com.qiujie.entity.FileTask;
import com.qiujie.entity.FileTaskError;
import com.qiujie.entity.Staff;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.DeptMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.DatetimeUtil;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.util.EnumUtil;
import com.qiujie.util.SecurityUtil;
import com.qiujie.vo.AttendanceMonthSummaryVO;
import com.qiujie.vo.AttendanceMonthVO;
import com.qiujie.vo.StaffAttendanceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
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

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

@Service
public class AttendanceService extends ServiceImpl<AttendanceMapper, Attendance> {

    private static final int DB_BATCH_SIZE = 200;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private DatetimeUtil datetimeUtil;

    @Autowired
    private FileTaskService fileTaskService;

    @Autowired
    private FileTaskErrorService fileTaskErrorService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ThreadPoolTaskExecutor fileTaskExecutor;

    @Autowired
    private FileTaskEngine fileTaskEngine;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private SecurityUtil securityUtil;

    public ResponseDTO add(Attendance attendance) {
        if (save(attendance)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO delete(Integer id) {
        if (removeById(id)) {
            return Response.success();
        }
        return Response.error();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO edit(Attendance attendance) {
        if (updateById(attendance)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO query(Integer id) {
        Attendance attendance = getById(id);
        if (attendance != null) {
            return Response.success(attendance);
        }
        return Response.error();
    }

    public ResponseDTO list(Integer current, Integer size, String name, Integer deptId, String month) {
        IPage<StaffAttendanceVO> config = new Page<>(current, size);
        if (name == null) {
            name = "";
        }
        IPage<StaffAttendanceVO> page;
        if (deptId == null) {
            page = this.staffMapper.listStaffAttendanceVO(config, name);
        } else {
            page = this.staffMapper.listStaffDeptAttendanceVO(config, name, deptId);
        }
        List<StaffAttendanceVO> staffDeptVOList = page.getRecords();
        if (month == null) {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }
        String[] monthDayList = this.datetimeUtil.getMonthDayList(month);

        // 批量加载：一次 SQL 查所有员工 x 所有日期
        List<Integer> staffIds = staffDeptVOList.stream().map(StaffAttendanceVO::getStaffId).collect(Collectors.toList());
        List<Date> dates = new ArrayList<>();
        for (String day : monthDayList) {
            dates.add(DateUtil.parse(day, "yyyyMMdd").toSqlDate());
        }
        Map<String, Attendance> attendanceMap = new HashMap<>();
        if (!staffIds.isEmpty() && !dates.isEmpty()) {
            List<Attendance> attendances = this.attendanceMapper.queryByStaffIdsAndDates(staffIds, dates);
            for (Attendance a : attendances) {
                attendanceMap.put(a.getStaffId() + "_" + DateUtil.format(a.getAttendanceDate(), "yyyyMMdd"), a);
            }
        }

        for (StaffAttendanceVO staffDeptVO : staffDeptVOList) {
            List<HashMap<String, Object>> list = new ArrayList<>();
            for (String day : monthDayList) {
                HashMap<String, Object> map = new HashMap<>();
                Attendance attendance = attendanceMap.get(staffDeptVO.getStaffId() + "_" + day);
                if (attendance == null) {
                    Date date = DateUtil.parse(day, "yyyyMMdd").toSqlDate();
                    if (DateUtil.isWeekend(date) || this.datetimeUtil.isHoliday(date)) {
                        map.put("message", AttendanceStatusEnum.LEAVE.getMessage());
                        map.put("tagType", AttendanceStatusEnum.LEAVE.getTagType());
                    } else {
                        map.put("message", AttendanceStatusEnum.NORMAL.getMessage());
                        map.put("tagType", AttendanceStatusEnum.NORMAL.getTagType());
                    }
                    map.put("attendanceDate", date);
                } else {
                    map.put("message", attendance.getStatus().getMessage());
                    map.put("tagType", attendance.getStatus().getTagType());
                    map.put("attendanceDate", attendance.getAttendanceDate());
                }
                list.add(map);
            }
            staffDeptVO.setAttendanceList(list);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", staffDeptVOList);
        map.put("dayNum", monthDayList.length);
        map.put("month", month);
        return Response.success(map);
    }

    public void export(HttpServletResponse response, String month, String filename) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8) + ".xlsx");
        DateTime dt = DateUtil.parse(month, "yyyyMM");
        Date exportStart = dt.toSqlDate();
        Date exportEnd = DateUtil.offsetMonth(dt, 1).toSqlDate();
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), AttendanceMonthVO.class).build();
        WriteSheet writeSheet = EasyExcel.writerSheet("attendance").build();
        try {
            int pageSize = 500;
            int current = 1;
            IPage<AttendanceMonthVO> page;
            do {
                page = new Page<>(current, pageSize);
                page = this.staffMapper.queryAttendanceMonthVOPage(page);
                List<AttendanceMonthVO> list = page.getRecords();
                if (list.isEmpty()) {
                    break;
                }
                List<Integer> staffIds = list.stream()
                        .map(AttendanceMonthVO::getStaffId)
                        .collect(Collectors.toList());
                Map<Integer, AttendanceMonthSummaryVO> summaryMap = new HashMap<>();
                if (!staffIds.isEmpty()) {
                    summaryMap = this.attendanceMapper.queryMonthSummaryByStaffIds(exportStart, exportEnd, staffIds).stream()
                            .collect(Collectors.toMap(AttendanceMonthSummaryVO::getStaffId, item -> item));
                }
                for (AttendanceMonthVO vo : list) {
                    AttendanceMonthSummaryVO summary = summaryMap.get(vo.getStaffId());
                    vo.setLateTimes(summary == null ? 0 : valueOrZero(summary.getLateTimes()));
                    vo.setLeaveEarlyTimes(summary == null ? 0 : valueOrZero(summary.getLeaveEarlyTimes()));
                    vo.setAbsenteeismTimes(summary == null ? 0 : valueOrZero(summary.getAbsenteeismTimes()));
                    vo.setLeaveDays(summary == null ? 0 : valueOrZero(summary.getLeaveDays()));
                    vo.setTimeOffDays(summary == null ? 0 : valueOrZero(summary.getTimeOffDays()));
                }
                excelWriter.write(list, writeSheet);
                current++;
            } while (current <= page.getPages());
        } finally {
            excelWriter.finish();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO imp(MultipartFile file) throws IOException {
        List<AttendanceImportRow> rows = EasyExcelUtil.read(file.getInputStream(), 2, AttendanceImportRow.class);
        int rowNum = 3;
        for (AttendanceImportRow row : rows) {
            row.setRowNum(rowNum++);
        }
        ImportBatchResult result = processImportRows(rows, 0L, false);
        if (result.failCount > 0) {
            return Response.error(BusinessStatusEnum.DATA_IMPORT_ERROR);
        }
        return Response.success();
    }

    // ==================== 异步导入入口 ====================
    // 通过三阶段上传完成后创建异步导入任务
    public ResponseDTO createImportTask(String uploadId) {
        String mergedKey = fileUploadService.completeUpload(uploadId);
        FileTask fileTask = fileTaskService.createTask(
                TaskTypeEnum.IMPORT,
                TaskModuleEnum.ATTENDANCE,
                "attendance_import.xlsx",
                mergedKey,
                null,
                getCurrentOperatorId()
        );
        fileTaskExecutor.execute(() -> runImportTask(fileTask.getId()));
        return Response.success("导入任务已创建", fileTask);
    }

    // ==================== 异步导出入口 ====================
    public ResponseDTO createExportTask(String month, String filename) {
        if (!StrUtil.isNotBlank(month)) {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }
        String exportName = StrUtil.isNotBlank(filename) ? filename : month + "_attendance_report.xlsx";
        if (!exportName.toLowerCase().endsWith(".xlsx")) {
            exportName = exportName + ".xlsx";
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("month", month);
        FileTask fileTask = fileTaskService.createTask(
                TaskTypeEnum.EXPORT,
                TaskModuleEnum.ATTENDANCE,
                exportName,
                null,
                JSON.toJSONString(queryParams),
                getCurrentOperatorId()
        );
        String finalMonth = month;
        String finalExportName = exportName;
        fileTaskExecutor.execute(() -> runExportTask(fileTask.getId(), finalMonth, finalExportName));
        return Response.success("导出任务已创建", fileTask);
    }

    public ResponseDTO setAttendance(Attendance attendance) {
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("staff_id", attendance.getStaffId()).eq("attendance_date", attendance.getAttendanceDate());
        if (update(attendance, queryWrapper) || save(attendance)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO queryAll() {
        List<Map<String, Object>> enumList = EnumUtil.getEnumList(AttendanceStatusEnum.class);
        for (Map<String, Object> map : enumList) {
            for (AttendanceStatusEnum attendanceStatusEnum : AttendanceStatusEnum.values()) {
                if (map.get("code") == attendanceStatusEnum.getCode()) {
                    map.put("tagType", attendanceStatusEnum.getTagType());
                }
            }
        }
        return Response.success(enumList);
    }

    public ResponseDTO queryByStaffIdAndDate(Integer id, String date) {
        Date sqlDate = DateUtil.parse(date.replace("-", ""), "yyyyMMdd").toSqlDate();
        Attendance attendance = this.attendanceMapper.queryByStaffIdAndDate(id, sqlDate);
        if (attendance != null) {
            return Response.success(attendance);
        }
        return Response.error();
    }

    // ==================== 异步导入核心 ====================
    // 委托给 FileTaskEngine 执行，考勤特有的校验逻辑由 AttendanceImportHandler 提供
    private void runImportTask(Long taskId) {
        fileTaskEngine.runImport(taskId, new AttendanceImportHandler());
    }

    // ==================== 异步导出核心 ====================
    // 委托给 FileTaskEngine 执行，考勤特有的分页查询由 AttendanceExportHandler 提供
    private void runExportTask(Long taskId, String month, String exportName) {
        FileTask task = fileTaskService.getById(taskId);
        String queryParamsJson = task != null ? task.getQueryParams() : "{\"month\":\"" + month + "\"}";
        fileTaskEngine.runExport(taskId, new AttendanceExportHandler(), queryParamsJson, exportName);
    }

    // ==================== 批量处理 ====================
    // 三步走：① 批量查询本批次涉及的员工/部门/已有考勤
    //        ② 逐行校验 + 计算考勤状态（迟到/早退/旷工/正常）
    //        ③ 批量 saveOrUpdate + 批量记录错误
    // 关键：先批量查再逐行判，避免每行一次 DB 查询
    ImportBatchResult processImportRows(List<AttendanceImportRow> rows, Long taskId, boolean persistTaskError) {
        ImportBatchResult result = new ImportBatchResult();
        if (rows.isEmpty()) {
            return result;
        }
        // ① 批量查询——从本批行中提取 ID 集合，一次性查出所有相关数据，避免 N+1
        Set<Integer> staffIds = rows.stream()
                .map(AttendanceImportRow::getStaffId)
                .filter(item -> item != null)
                .collect(Collectors.toSet());
        Map<Integer, Staff> staffMap = staffIds.isEmpty()
                ? new HashMap<>()
                : this.staffMapper.selectBatchIds(staffIds).stream().collect(Collectors.toMap(Staff::getId, item -> item));
        Set<Integer> deptIds = staffMap.values().stream()
                .map(Staff::getDeptId)
                .filter(item -> item != null)
                .collect(Collectors.toSet());
        Map<Integer, Dept> deptMap = deptIds.isEmpty()
                ? new HashMap<>()
                : this.deptMapper.selectBatchIds(deptIds).stream().collect(Collectors.toMap(Dept::getId, item -> item));
        Set<Date> attendanceDates = rows.stream()
                .map(AttendanceImportRow::getAttendanceDate)
                .filter(item -> item != null)
                .map(this::toSqlDate)
                .collect(Collectors.toCollection(HashSet::new));
        // 查出已有考勤记录（同一员工同一天只留一条，防止重复导入）
        Map<String, Attendance> existingMap = new HashMap<>();
        if (!staffIds.isEmpty() && !attendanceDates.isEmpty()) {
            existingMap = this.attendanceMapper.queryByStaffIdsAndDates(staffIds, attendanceDates).stream()
                    .collect(Collectors.toMap(this::buildAttendanceKey, item -> item, (left, right) -> left));
        }

        // ② 逐行校验
        List<Attendance> saveList = new ArrayList<>();
        List<FileTaskError> errors = new ArrayList<>();
        for (AttendanceImportRow row : rows) {
            String rowData = buildRowData(row);
            if (row.getStaffId() == null) {
                errors.add(buildTaskError(taskId, resolveRowNum(row), rowData, "员工id不能为空"));
                continue;
            }
            if (row.getAttendanceDate() == null) {
                errors.add(buildTaskError(taskId, resolveRowNum(row), rowData, "考勤日期不能为空"));
                continue;
            }
            Staff staff = staffMap.get(row.getStaffId());
            if (staff == null) {
                errors.add(buildTaskError(taskId, resolveRowNum(row), rowData, "员工不存在"));
                continue;
            }
            Dept dept = deptMap.get(staff.getDeptId());
            if (dept == null) {
                errors.add(buildTaskError(taskId, resolveRowNum(row), rowData, "员工部门不存在"));
                continue;
            }
            Attendance attendance = convertAttendance(row);
            // 周末不需要考勤
            if (DateUtil.isWeekend(attendance.getAttendanceDate())) {
                result.successCount++;
                continue;
            }
            // 已存在休假/调休记录，不覆盖
            Attendance existing = existingMap.get(buildAttendanceKey(attendance));
            if (existing != null && (existing.getStatus() == AttendanceStatusEnum.LEAVE || existing.getStatus() == AttendanceStatusEnum.TIME_OFF)) {
                result.successCount++;
                continue;
            }
            // 已有记录则需要基于原 ID 做 update 而非 insert
            if (existing != null) {
                attendance.setId(existing.getId());
            }
            // 对比部门上班时间来判定考勤状态
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

        // ③ 批量写入——每 200 行一个事务，避免大事务锁表
        if (!saveList.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> saveOrUpdateBatch(saveList, DB_BATCH_SIZE));
            result.successCount += saveList.size();
        }
        if (!errors.isEmpty() && persistTaskError) {
            fileTaskErrorService.saveBatch(errors, DB_BATCH_SIZE);
        }
        result.failCount += errors.size();
        result.totalCount = rows.size();
        result.processedCount = rows.size();
        // 原子更新进度，触发 SSE 推送
        if (taskId > 0) {
            fileTaskService.increaseProgress(taskId, result.totalCount, result.processedCount, result.successCount, result.failCount);
        }
        return result;
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

    private FileTaskError buildTaskError(Long taskId, Integer rowNum, String rawData, String errorMessage) {
        return new FileTaskError()
                .setTaskId(taskId)
                .setRowNum(rowNum)
                .setRawData(rawData)
                .setErrorMessage(errorMessage);
    }

    private String buildRowData(AttendanceImportRow row) {
        String date = row.getAttendanceDate() == null ? "" : DateUtil.formatDate(row.getAttendanceDate());
        return "staffId=" + row.getStaffId() + ", attendanceDate=" + date;
    }

    private Integer resolveRowNum(AttendanceImportRow row) {
        return row.getRowNum() == null ? 0 : row.getRowNum();
    }

    private String buildAttendanceKey(Attendance attendance) {
        return attendance.getStaffId() + "_" + attendance.getAttendanceDate();
    }

    private Timestamp toSqlTimestamp(java.util.Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        LocalDateTime localDateTime = LocalDateTime.of(1970, 1, 1,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
        return Timestamp.valueOf(localDateTime);
    }

    private Date toSqlDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime());
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer getCurrentOperatorId() {
        return securityUtil.getCurrentOperatorId();
    }

    boolean isLate(Attendance attendance, Dept dept) {
        if (attendance.getMorStartTime() == null || attendance.getAftStartTime() == null) {
            return false;
        }
        if (DateUtil.compare(attendance.getMorStartTime(), dept.getMorStartTime(), "HH:mm:ss") > 0) {
            return true;
        }
        return DateUtil.compare(attendance.getAftStartTime(), dept.getAftStartTime(), "HH:mm:ss") > 0;
    }

    boolean isLeaveEarly(Attendance attendance, Dept dept) {
        if (attendance.getMorEndTime() == null || attendance.getAftEndTime() == null) {
            return false;
        }
        if (DateUtil.compare(attendance.getMorEndTime(), dept.getMorEndTime(), "HH:mm:ss") < 0) {
            return true;
        }
        return DateUtil.compare(attendance.getAftEndTime(), dept.getAftEndTime(), "HH:mm:ss") < 0;
    }

    boolean isAbsenteeism(Attendance attendance, Dept dept) {
        if (attendance.getMorStartTime() == null || attendance.getMorEndTime() == null
                || attendance.getAftStartTime() == null || attendance.getAftEndTime() == null) {
            return true;
        }
        return isLate(attendance, dept) && isLeaveEarly(attendance, dept);
    }

    static class ImportBatchResult {
        int totalCount;
        int processedCount;
        int successCount;
        int failCount;
    }

    // ==================== 异步导入处理器（供 FileTaskEngine 回调） ====================
    private class AttendanceImportHandler implements ImportProcessor<AttendanceImportRow> {

        @Override
        public Class<AttendanceImportRow> getRowClass() {
            return AttendanceImportRow.class;
        }

        @Override
        public TaskModuleEnum getModule() {
            return TaskModuleEnum.ATTENDANCE;
        }

        @Override
        public void processBatch(List<AttendanceImportRow> rows, Long taskId, Consumer<FileTaskError> errorCollector) {
            if (rows.isEmpty()) {
                return;
            }
            // ① 批量预加载关联数据，避免 N+1
            Set<Integer> staffIds = rows.stream()
                    .map(AttendanceImportRow::getStaffId)
                    .filter(item -> item != null)
                    .collect(Collectors.toSet());
            Map<Integer, Staff> staffMap = staffIds.isEmpty()
                    ? new HashMap<>()
                    : AttendanceService.this.staffMapper.selectBatchIds(staffIds).stream()
                            .collect(Collectors.toMap(Staff::getId, item -> item));
            Set<Integer> deptIds = staffMap.values().stream()
                    .map(Staff::getDeptId)
                    .filter(item -> item != null)
                    .collect(Collectors.toSet());
            Map<Integer, Dept> deptMap = deptIds.isEmpty()
                    ? new HashMap<>()
                    : AttendanceService.this.deptMapper.selectBatchIds(deptIds).stream()
                            .collect(Collectors.toMap(Dept::getId, item -> item));
            Set<Date> attendanceDates = rows.stream()
                    .map(AttendanceImportRow::getAttendanceDate)
                    .filter(item -> item != null)
                    .map(AttendanceService.this::toSqlDate)
                    .collect(Collectors.toCollection(HashSet::new));
            Map<String, Attendance> existingMap = new HashMap<>();
            if (!staffIds.isEmpty() && !attendanceDates.isEmpty()) {
                existingMap = AttendanceService.this.attendanceMapper
                        .queryByStaffIdsAndDates(staffIds, attendanceDates).stream()
                        .collect(Collectors.toMap(AttendanceService.this::buildAttendanceKey, item -> item, (left, right) -> left));
            }

            // ② 逐行校验 + 状态判定
            List<Attendance> saveList = new ArrayList<>();
            for (AttendanceImportRow row : rows) {
                String rowData = buildRowData(row);
                int rowNum = resolveRowNum(row);
                if (row.getStaffId() == null) {
                    errorCollector.accept(buildTaskError(taskId, rowNum, rowData, "员工id不能为空"));
                    continue;
                }
                if (row.getAttendanceDate() == null) {
                    errorCollector.accept(buildTaskError(taskId, rowNum, rowData, "考勤日期不能为空"));
                    continue;
                }
                Staff staff = staffMap.get(row.getStaffId());
                if (staff == null) {
                    errorCollector.accept(buildTaskError(taskId, rowNum, rowData, "员工不存在"));
                    continue;
                }
                Dept dept = deptMap.get(staff.getDeptId());
                if (dept == null) {
                    errorCollector.accept(buildTaskError(taskId, rowNum, rowData, "员工部门不存在"));
                    continue;
                }
                Attendance attendance = convertAttendance(row);
                if (DateUtil.isWeekend(attendance.getAttendanceDate())) {
                    continue;
                }
                Attendance existing = existingMap.get(buildAttendanceKey(attendance));
                if (existing != null && (existing.getStatus() == AttendanceStatusEnum.LEAVE
                        || existing.getStatus() == AttendanceStatusEnum.TIME_OFF)) {
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

            // ③ 批量写入——每 200 行一个事务
            if (!saveList.isEmpty()) {
                transactionTemplate.executeWithoutResult(status -> saveOrUpdateBatch(saveList, DB_BATCH_SIZE));
            }
        }
    }

    // ==================== 异步导出处理器（供 FileTaskEngine 回调） ====================
    private class AttendanceExportHandler implements ExportProcessor<AttendanceMonthVO> {

        @Override
        public Class<AttendanceMonthVO> getRowClass() {
            return AttendanceMonthVO.class;
        }

        @Override
        public TaskModuleEnum getModule() {
            return TaskModuleEnum.ATTENDANCE;
        }

        @Override
        public IPage<AttendanceMonthVO> queryPage(int current, int pageSize, String queryParamsJson) {
            String month = parseMonth(queryParamsJson);
            DateTime dt = DateUtil.parse(month, "yyyyMM");
            Date rangeStart = dt.toSqlDate();
            Date rangeEnd = DateUtil.offsetMonth(dt, 1).toSqlDate();
            IPage<AttendanceMonthVO> page = new Page<>(current, pageSize);
            page = AttendanceService.this.staffMapper.queryAttendanceMonthVOPage(page);
            List<AttendanceMonthVO> list = page.getRecords();
            if (!list.isEmpty()) {
                List<Integer> staffIds = list.stream()
                        .map(AttendanceMonthVO::getStaffId)
                        .collect(Collectors.toList());
                Map<Integer, AttendanceMonthSummaryVO> summaryMap = AttendanceService.this.attendanceMapper
                        .queryMonthSummaryByStaffIds(rangeStart, rangeEnd, staffIds).stream()
                        .collect(Collectors.toMap(AttendanceMonthSummaryVO::getStaffId, item -> item));
                for (AttendanceMonthVO vo : list) {
                    AttendanceMonthSummaryVO summary = summaryMap.get(vo.getStaffId());
                    vo.setLateTimes(summary == null ? 0 : valueOrZero(summary.getLateTimes()));
                    vo.setLeaveEarlyTimes(summary == null ? 0 : valueOrZero(summary.getLeaveEarlyTimes()));
                    vo.setAbsenteeismTimes(summary == null ? 0 : valueOrZero(summary.getAbsenteeismTimes()));
                    vo.setLeaveDays(summary == null ? 0 : valueOrZero(summary.getLeaveDays()));
                    vo.setTimeOffDays(summary == null ? 0 : valueOrZero(summary.getTimeOffDays()));
                }
            }
            return page;
        }

        private String parseMonth(String queryParamsJson) {
            if (queryParamsJson == null || queryParamsJson.isEmpty()) {
                return DateUtil.format(new java.util.Date(), "yyyyMM");
            }
            try {
                Map<String, Object> params = JSON.parseObject(queryParamsJson, Map.class);
                return params.getOrDefault("month", DateUtil.format(new java.util.Date(), "yyyyMM")).toString();
            } catch (Exception e) {
                return DateUtil.format(new java.util.Date(), "yyyyMM");
            }
        }
    }
}
