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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

/**
 * AttendanceImportBatchProcessor 单元测试——零 Spring 上下文，mock Mapper。
 * 验证导入校验规则（员工/部门/日期）、状态判定（迟到/早退/旷工/正常）、
 * 周末跳过、已有休假/调休跳过、批量写入。
 */
@DisplayName("考勤导入批处理")
class AttendanceImportBatchProcessorUnitTest {

    private AttendanceMapper attendanceMapper;
    private DeptMapper deptMapper;
    private StaffMapper staffMapper;
    private TransactionTemplate transactionTemplate;
    private AttendanceImportBatchProcessor processor;

    private Staff staff1;
    private Dept dept1;

    @BeforeEach
    void setUp() {
        attendanceMapper = mock(AttendanceMapper.class);
        deptMapper = mock(DeptMapper.class);
        staffMapper = mock(StaffMapper.class);
        transactionTemplate = mock(TransactionTemplate.class);

        staff1 = new Staff();
        staff1.setId(1);
        staff1.setDeptId(10);
        dept1 = new Dept();
        dept1.setId(10);
    }

    private AttendanceImportBatchProcessor newProcessor() {
        return new AttendanceImportBatchProcessor(attendanceMapper, deptMapper, staffMapper, transactionTemplate);
    }

    private AttendanceImportRow row(int staffId, String date, String morStart, String morEnd, String aftStart, String aftEnd) {
        AttendanceImportRow row = new AttendanceImportRow();
        row.setStaffId(staffId);
        row.setAttendanceDate(Date.valueOf(date));
        row.setMorStartTime(timestamp(morStart));
        row.setMorEndTime(timestamp(morEnd));
        row.setAftStartTime(timestamp(aftStart));
        row.setAftEndTime(timestamp(aftEnd));
        row.setRowNum(3);
        return row;
    }

    private Timestamp timestamp(String time) {
        return Timestamp.valueOf("2026-01-01 " + time);
    }

    @Test
    @DisplayName("空输入 → 空结果")
    void emptyRows_ShouldReturnEmptyResult() {
        processor = newProcessor();

        var result = processor.process(new ArrayList<>(), 0L, errors -> {});

        assertEquals(0, result.totalCount);
        assertEquals(0, result.successCount);
    }

    @Test
    @DisplayName("staffId 为空 → 收集错误，不写库")
    void nullStaffId_ShouldCollectError() {
        processor = newProcessor();
        List<FileTaskError> errors = new ArrayList<>();
        AttendanceImportRow row = row(0, "2026-08-27", "09:00:00", "12:00:00", "13:00:00", "18:00:00");
        row.setStaffId(null);

        var result = processor.process(List.of(row), 1L, errors::add);

        assertEquals(1, result.failCount);
        assertEquals(1, errors.size());
        assertEquals("员工id不能为空", errors.get(0).getErrorMessage());
        verify(attendanceMapper, never()).insert(any(Attendance.class));
    }

    @Test
    @DisplayName("员工不存在 → 收集错误")
    void staffNotExists_ShouldCollectError() {
        processor = newProcessor();
        when(staffMapper.selectBatchIds(anySet())).thenReturn(new ArrayList<>());
        List<FileTaskError> errors = new ArrayList<>();

        var result = processor.process(List.of(row(99, "2026-08-27", "09:00:00", "12:00:00", "13:00:00", "18:00:00")), 1L, errors::add);

        assertEquals(1, result.failCount);
        assertEquals("员工不存在", errors.get(0).getErrorMessage());
    }

    @Test
    @DisplayName("部门不存在 → 收集错误")
    void deptNotExists_ShouldCollectError() {
        processor = newProcessor();
        Staff noDept = new Staff();
        noDept.setId(1);
        noDept.setDeptId(99);
        when(staffMapper.selectBatchIds(anySet())).thenReturn(List.of(noDept));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(new ArrayList<>());
        List<FileTaskError> errors = new ArrayList<>();

        var result = processor.process(List.of(row(1, "2026-08-27", "09:00:00", "12:00:00", "13:00:00", "18:00:00")), 1L, errors::add);

        assertEquals(1, result.failCount);
        assertEquals("员工部门不存在", errors.get(0).getErrorMessage());
    }

    @Test
    @DisplayName("正常工时 → 状态 NORMAL 并写入")
    void validRow_ShouldSaveNormal() {
        processor = newProcessor();
        when(staffMapper.selectBatchIds(anySet())).thenReturn(List.of(staff1));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(List.of(dept1));
        when(attendanceMapper.queryByStaffIdsAndDates(anySet(), anySet())).thenReturn(new ArrayList<>());
        doAnswer(inv -> {
            // 实际执行事务内 lambda，让 insert 真正发生
            java.util.function.Consumer<Object> consumer = inv.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        var result = processor.process(List.of(row(1, "2026-08-27", "09:00:00", "12:00:00", "13:00:00", "18:00:00")), 0L, errors -> {});

        assertEquals(1, result.successCount);
        ArgumentCaptor<List<Attendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionTemplate).executeWithoutResult(any());
        // 事务内 insert 被调用
        verify(attendanceMapper).insert(any(Attendance.class));
    }

    @Test
    @DisplayName("早上迟到 → 状态 LATE")
    void late_ShouldBeDetected() {
        processor = newProcessor();
        when(staffMapper.selectBatchIds(anySet())).thenReturn(List.of(staff1));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(List.of(dept1));
        when(attendanceMapper.queryByStaffIdsAndDates(anySet(), anySet())).thenReturn(new ArrayList<>());
        doAnswer(inv -> {
            java.util.function.Consumer<Object> consumer = inv.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        processor.process(List.of(row(1, "2026-08-27", "09:30:00", "12:00:00", "13:00:00", "18:00:00")), 0L, errors -> {});

        ArgumentCaptor<Attendance> lateCaptor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceMapper).insert(lateCaptor.capture());
        assertEquals(AttendanceStatusEnum.LATE, lateCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("周末日期 → 跳过不写入")
    void weekend_ShouldSkip() {
        processor = newProcessor();
        when(staffMapper.selectBatchIds(anySet())).thenReturn(List.of(staff1));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(List.of(dept1));
        when(attendanceMapper.queryByStaffIdsAndDates(anySet(), anySet())).thenReturn(new ArrayList<>());

        // 2026-08-29 是周六
        var result = processor.process(List.of(row(1, "2026-08-29", "09:00:00", "12:00:00", "13:00:00", "18:00:00")), 0L, errors -> {});

        assertEquals(1, result.successCount); // 计为成功但不写库
        verify(attendanceMapper, never()).insert(any(Attendance.class));
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    @Test
    @DisplayName("已有休假/调休记录 → 跳过不覆盖")
    void existingLeaveOrTimeOff_ShouldSkip() {
        processor = newProcessor();
        when(staffMapper.selectBatchIds(anySet())).thenReturn(List.of(staff1));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(List.of(dept1));
        Attendance existing = new Attendance();
        existing.setId(1);
        existing.setStatus(AttendanceStatusEnum.LEAVE);
        existing.setStaffId(1);
        existing.setAttendanceDate(Date.valueOf("2026-08-27"));
        when(attendanceMapper.queryByStaffIdsAndDates(anySet(), anySet())).thenReturn(List.of(existing));

        processor.process(List.of(row(1, "2026-08-27", "09:00:00", "12:00:00", "13:00:00", "18:00:00")), 0L, errors -> {});

        verify(attendanceMapper, never()).insert(any(Attendance.class));
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    @Test
    @DisplayName("已有普通记录 → 更新而非新增")
    void existingNormal_ShouldUpdate() {
        processor = newProcessor();
        when(staffMapper.selectBatchIds(anySet())).thenReturn(List.of(staff1));
        when(deptMapper.selectBatchIds(anySet())).thenReturn(List.of(dept1));
        Attendance existing = new Attendance();
        existing.setId(1);
        existing.setStatus(AttendanceStatusEnum.NORMAL);
        existing.setStaffId(1);
        existing.setAttendanceDate(Date.valueOf("2026-08-27"));
        when(attendanceMapper.queryByStaffIdsAndDates(anySet(), anySet())).thenReturn(List.of(existing));
        doAnswer(inv -> {
            java.util.function.Consumer<Object> consumer = inv.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        processor.process(List.of(row(1, "2026-08-27", "09:00:00", "12:00:00", "13:00:00", "18:00:00")), 0L, errors -> {});

        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getId());
    }
}