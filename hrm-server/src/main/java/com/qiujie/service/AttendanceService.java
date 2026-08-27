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
import com.qiujie.dto.AttendanceImportRow;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.Dept;
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
import java.util.List;
import java.util.Map;
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
    private FileTaskErrorService fileTaskErrorService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FileTaskCoordinator fileTaskCoordinator;

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
        FileTaskCoordinator.TaskSubmission submission = fileTaskCoordinator.submitImport(
                new FileTaskCoordinator.ImportCommand(
                        TaskModuleEnum.ATTENDANCE,
                        "attendance_import.xlsx",
                        mergedKey,
                        null,
                        getCurrentOperatorId()),
                new AttendanceImportHandler());
        return Response.success("导入任务已创建", submission.snapshot());
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
        FileTaskCoordinator.TaskSubmission submission = fileTaskCoordinator.submitExport(
                new FileTaskCoordinator.ExportCommand(
                        TaskModuleEnum.ATTENDANCE,
                        exportName,
                        JSON.toJSONString(queryParams),
                        getCurrentOperatorId()),
                new AttendanceExportHandler());
        return Response.success("导出任务已创建", submission.snapshot());
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

    private AttendanceImportBatchProcessor importBatchProcessor() {
        return new AttendanceImportBatchProcessor(attendanceMapper, deptMapper, staffMapper, transactionTemplate);
    }

    // 三步走：① 批量查询本批次涉及的员工/部门/已有考勤
    //        ② 逐行校验 + 计算考勤状态（迟到/早退/旷工/正常）
    //        ③ 批量 saveOrUpdate + 批量记录错误
    // 关键：先批量查再逐行判，避免每行一次 DB 查询
    ImportBatchResult processImportRows(List<AttendanceImportRow> rows, Long taskId, boolean persistTaskError) {
        List<FileTaskError> errors = new ArrayList<>();
        AttendanceImportBatchProcessor.Result processed = importBatchProcessor().process(
                rows, taskId, errors::add);
        if (persistTaskError && !errors.isEmpty()) {
            fileTaskErrorService.saveBatch(errors, DB_BATCH_SIZE);
        }
        ImportBatchResult result = new ImportBatchResult();
        result.totalCount = processed.totalCount;
        result.processedCount = processed.processedCount;
        result.successCount = processed.successCount;
        result.failCount = processed.failCount;
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
            importBatchProcessor().process(rows, taskId, errorCollector);
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
