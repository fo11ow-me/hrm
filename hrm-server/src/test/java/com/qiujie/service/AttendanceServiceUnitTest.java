package com.qiujie.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.Dept;
import com.qiujie.entity.FileTaskError;
import com.qiujie.entity.Staff;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.DeptMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AttendanceService 单元测试。
 * 重点测试考勤导入校验逻辑：迟到/早退/旷工判定、批量预加载、错误收集。
 *
 * @author qiujie
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceUnitTest {

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private StaffMapper staffMapper;

    @Mock
    private DeptMapper deptMapper;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private FileTaskService fileTaskService;

    @Mock
    private FileTaskErrorService fileTaskErrorService;

    @Mock
    private SecurityUtil securityUtil;

    private AttendanceService attendanceService;

    private Dept dept1;
    private Staff staff1;
    private Staff staff2;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService();
        // 通过反射注入 mock 依赖（父类 private 字段无法直接访问）
        ReflectionTestUtils.setField(attendanceService, "attendanceMapper", attendanceMapper);
        ReflectionTestUtils.setField(attendanceService, "staffMapper", staffMapper);
        ReflectionTestUtils.setField(attendanceService, "deptMapper", deptMapper);
        ReflectionTestUtils.setField(attendanceService, "transactionTemplate", transactionTemplate);
        ReflectionTestUtils.setField(attendanceService, "fileTaskService", fileTaskService);
        ReflectionTestUtils.setField(attendanceService, "fileTaskErrorService", fileTaskErrorService);
        ReflectionTestUtils.setField(attendanceService, "securityUtil", securityUtil);

        // 准备测试部门：技术部 09:00-12:00, 13:00-18:00
        dept1 = new Dept();
        dept1.setId(1);
        dept1.setName("技术部");
        dept1.setMorStartTime(parseTimestamp("09:00:00"));
        dept1.setMorEndTime(parseTimestamp("12:00:00"));
        dept1.setAftStartTime(parseTimestamp("13:00:00"));
        dept1.setAftEndTime(parseTimestamp("18:00:00"));

        staff1 = new Staff();
        staff1.setId(1);
        staff1.setName("张三");
        staff1.setDeptId(1);

        staff2 = new Staff();
        staff2.setId(2);
        staff2.setName("李四");
        staff2.setDeptId(1);
    }

    // ==================== isLate 测试 ====================

    @Test
    void isLate_MorStartAfterDeptMorStart_ShouldReturnTrue() {
        Attendance attendance = new Attendance()
                .setMorStartTime(parseTimestamp("09:30:00"))
                .setAftStartTime(parseTimestamp("13:00:00"));

        assertThat(attendanceService.isLate(attendance, dept1)).isTrue();
    }

    @Test
    void isLate_AftStartAfterDeptAftStart_ShouldReturnTrue() {
        Attendance attendance = new Attendance()
                .setMorStartTime(parseTimestamp("09:00:00"))
                .setAftStartTime(parseTimestamp("13:30:00"));

        assertThat(attendanceService.isLate(attendance, dept1)).isTrue();
    }

    @Test
    void isLate_BothOnTime_ShouldReturnFalse() {
        Attendance attendance = new Attendance()
                .setMorStartTime(parseTimestamp("08:55:00"))
                .setAftStartTime(parseTimestamp("12:55:00"));

        assertThat(attendanceService.isLate(attendance, dept1)).isFalse();
    }

    @Test
    void isLate_MissingTimes_ShouldReturnFalse() {
        Attendance attendance = new Attendance()
                .setMorStartTime(null)
                .setAftStartTime(parseTimestamp("13:00:00"));

        assertThat(attendanceService.isLate(attendance, dept1)).isFalse();
    }

    // ==================== isLeaveEarly 测试 ====================

    @Test
    void isLeaveEarly_MorEndBeforeDeptMorEnd_ShouldReturnTrue() {
        Attendance attendance = new Attendance()
                .setMorEndTime(parseTimestamp("11:30:00"))
                .setAftEndTime(parseTimestamp("18:00:00"));

        assertThat(attendanceService.isLeaveEarly(attendance, dept1)).isTrue();
    }

    @Test
    void isLeaveEarly_AftEndBeforeDeptAftEnd_ShouldReturnTrue() {
        Attendance attendance = new Attendance()
                .setMorEndTime(parseTimestamp("12:00:00"))
                .setAftEndTime(parseTimestamp("17:30:00"));

        assertThat(attendanceService.isLeaveEarly(attendance, dept1)).isTrue();
    }

    @Test
    void isLeaveEarly_BothAfterEnd_ShouldReturnFalse() {
        Attendance attendance = new Attendance()
                .setMorEndTime(parseTimestamp("12:05:00"))
                .setAftEndTime(parseTimestamp("18:05:00"));

        assertThat(attendanceService.isLeaveEarly(attendance, dept1)).isFalse();
    }

    // ==================== isAbsenteeism 测试 ====================

    @Test
    void isAbsenteeism_MissingAllTimes_ShouldReturnTrue() {
        Attendance attendance = new Attendance()
                .setMorStartTime(null)
                .setMorEndTime(null)
                .setAftStartTime(null)
                .setAftEndTime(null);

        assertThat(attendanceService.isAbsenteeism(attendance, dept1)).isTrue();
    }

    @Test
    void isAbsenteeism_MissingMorStart_ShouldReturnTrue() {
        Attendance attendance = new Attendance()
                .setMorStartTime(null)
                .setMorEndTime(parseTimestamp("12:00:00"))
                .setAftStartTime(parseTimestamp("13:00:00"))
                .setAftEndTime(parseTimestamp("18:00:00"));

        assertThat(attendanceService.isAbsenteeism(attendance, dept1)).isTrue();
    }

    @Test
    void isAbsenteeism_LateAndLeaveEarly_ShouldReturnTrue() {
        Attendance attendance = new Attendance()
                .setMorStartTime(parseTimestamp("09:30:00"))
                .setMorEndTime(parseTimestamp("12:00:00"))
                .setAftStartTime(parseTimestamp("13:00:00"))
                .setAftEndTime(parseTimestamp("17:00:00"));

        assertThat(attendanceService.isAbsenteeism(attendance, dept1)).isTrue();
    }

    @Test
    void isAbsenteeism_AllTimesValid_ShouldReturnFalse() {
        Attendance attendance = new Attendance()
                .setMorStartTime(parseTimestamp("09:00:00"))
                .setMorEndTime(parseTimestamp("12:00:00"))
                .setAftStartTime(parseTimestamp("13:00:00"))
                .setAftEndTime(parseTimestamp("18:00:00"));

        assertThat(attendanceService.isAbsenteeism(attendance, dept1)).isFalse();
    }

    // ==================== processImportRows 测试 ====================

    @Test
    void processImportRows_EmptyRows_ShouldReturnEmptyResult() {
        AttendanceService.ImportBatchResult result =
                attendanceService.processImportRows(Collections.emptyList(), 0L, false);

        assertThat(result.totalCount).isZero();
    }

    @Test
    void processImportRows_NullStaffId_ShouldCollectError() {
        com.qiujie.dto.AttendanceImportRow row = new com.qiujie.dto.AttendanceImportRow();
        row.setStaffId(null);
        row.setAttendanceDate(parseDate("20240102"));
        row.setRowNum(3);

        AttendanceService.ImportBatchResult result =
                attendanceService.processImportRows(Arrays.asList(row), 1L, true);

        assertThat(result.failCount).isEqualTo(1);
        verify(fileTaskErrorService).saveBatch(argThat(errors ->
                !errors.isEmpty() && "员工id不能为空".equals(errors.iterator().next().getErrorMessage())), anyInt());
    }

    @Test
    void processImportRows_NullAttendanceDate_ShouldCollectError() {
        com.qiujie.dto.AttendanceImportRow row = new com.qiujie.dto.AttendanceImportRow();
        row.setStaffId(1);
        row.setAttendanceDate(null);
        row.setRowNum(3);

        when(staffMapper.selectBatchIds(anySet())).thenReturn(Arrays.asList(staff1));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(Arrays.asList(dept1));
        lenient().when(attendanceMapper.queryByStaffIdsAndDates(anySet(), anySet())).thenReturn(new ArrayList<>());

        AttendanceService.ImportBatchResult result =
                attendanceService.processImportRows(Arrays.asList(row), 1L, true);

        assertThat(result.failCount).isEqualTo(1);
    }

    @Test
    void processImportRows_StaffNotExists_ShouldCollectError() {
        com.qiujie.dto.AttendanceImportRow row = new com.qiujie.dto.AttendanceImportRow();
        row.setStaffId(99);
        row.setAttendanceDate(parseDate("20240102"));
        row.setRowNum(3);

        when(staffMapper.selectBatchIds(anySet())).thenReturn(new ArrayList<>());

        AttendanceService.ImportBatchResult result =
                attendanceService.processImportRows(Arrays.asList(row), 1L, true);

        assertThat(result.failCount).isEqualTo(1);
        verify(fileTaskErrorService).saveBatch(argThat(errors ->
                !errors.isEmpty() && "员工不存在".equals(errors.iterator().next().getErrorMessage())), anyInt());
    }

    @Test
    void processImportRows_DeptNotExists_ShouldCollectError() {
        com.qiujie.dto.AttendanceImportRow row = new com.qiujie.dto.AttendanceImportRow();
        row.setStaffId(1);
        row.setAttendanceDate(parseDate("20240102"));
        row.setRowNum(3);

        // staff 的 deptId 存在但 dept 表中没有
        Staff staffNoDept = new Staff();
        staffNoDept.setId(1);
        staffNoDept.setName("张三");
        staffNoDept.setDeptId(99);

        when(staffMapper.selectBatchIds(anySet())).thenReturn(Arrays.asList(staffNoDept));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(new ArrayList<>());

        AttendanceService.ImportBatchResult result =
                attendanceService.processImportRows(Arrays.asList(row), 1L, true);

        assertThat(result.failCount).isEqualTo(1);
        verify(fileTaskErrorService).saveBatch(argThat(errors ->
                !errors.isEmpty() && "员工部门不存在".equals(errors.iterator().next().getErrorMessage())), anyInt());
    }

    @Test
    void processImportRows_ValidRow_ShouldSaveAttendance() {
        com.qiujie.dto.AttendanceImportRow row = new com.qiujie.dto.AttendanceImportRow();
        row.setStaffId(1);
        row.setAttendanceDate(parseDate("20240102")); // 周二
        row.setMorStartTime(parseUtilDate("09:00:00"));
        row.setMorEndTime(parseUtilDate("12:00:00"));
        row.setAftStartTime(parseUtilDate("13:00:00"));
        row.setAftEndTime(parseUtilDate("18:00:00"));
        row.setRowNum(3);

        when(staffMapper.selectBatchIds(anySet())).thenReturn(Arrays.asList(staff1));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(Arrays.asList(dept1));
        when(attendanceMapper.queryByStaffIdsAndDates(anySet(), anySet())).thenReturn(new ArrayList<>());
        doAnswer(invocation -> null).when(transactionTemplate).executeWithoutResult(any());

        AttendanceService.ImportBatchResult result =
                attendanceService.processImportRows(Arrays.asList(row), 1L, false);

        assertThat(result.successCount).isEqualTo(1);
        assertThat(result.failCount).isZero();
    }

    @Test
    void processImportRows_LateStatus_ShouldBeDetected() {
        com.qiujie.dto.AttendanceImportRow row = new com.qiujie.dto.AttendanceImportRow();
        row.setStaffId(1);
        row.setAttendanceDate(parseDate("20240102"));
        row.setMorStartTime(parseUtilDate("09:30:00"));
        row.setMorEndTime(parseUtilDate("12:00:00"));
        row.setAftStartTime(parseUtilDate("13:00:00"));
        row.setAftEndTime(parseUtilDate("18:00:00"));
        row.setRowNum(3);

        when(staffMapper.selectBatchIds(anySet())).thenReturn(Arrays.asList(staff1));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(Arrays.asList(dept1));
        when(attendanceMapper.queryByStaffIdsAndDates(anySet(), anySet())).thenReturn(new ArrayList<>());
        doAnswer(invocation -> null).when(transactionTemplate).executeWithoutResult(any());

        attendanceService.processImportRows(Arrays.asList(row), 0L, false);

        ArgumentCaptor<List<Attendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionTemplate).executeWithoutResult(any());
    }

    // ==================== helper methods ====================

    private java.sql.Timestamp parseTimestamp(String timeStr) {
        try {
            java.util.Date d = timeFormat.parse(timeStr);
            return new java.sql.Timestamp(d.getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    private java.util.Date parseUtilDate(String timeStr) {
        try {
            return timeFormat.parse(timeStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private java.util.Date parseDate(String dateStr) {
        try {
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
}
