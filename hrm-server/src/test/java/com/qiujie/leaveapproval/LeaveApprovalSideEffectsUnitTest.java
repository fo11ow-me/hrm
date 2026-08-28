package com.qiujie.leaveapproval;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.StaffLeave;
import com.qiujie.entity.StaffOvertime;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.LeaveEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import com.qiujie.mapper.StaffOvertimeMapper;
import com.qiujie.service.AttendanceService;
import com.qiujie.util.DatetimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 请假审批副作用单测——零 Spring/Flowable 上下文，mock 三个依赖。
 * 验证考勤同步规则（逐日、跳过周末/节假日、调休扣减、新增/更新）。
 */
@DisplayName("请假审批副作用")
class LeaveApprovalSideEffectsUnitTest {

    private static final int STAFF_ID = 9;

    private AttendanceService attendanceService;
    private StaffOvertimeMapper staffOvertimeMapper;
    private DatetimeUtil datetimeUtil;
    private LeaveApprovalSideEffects sideEffects;

    @BeforeEach
    void setUp() {
        attendanceService = mock(AttendanceService.class);
        staffOvertimeMapper = mock(StaffOvertimeMapper.class);
        datetimeUtil = mock(DatetimeUtil.class);
        sideEffects = new LeaveApprovalSideEffectsImpl(attendanceService, staffOvertimeMapper, datetimeUtil);
        when(datetimeUtil.isHoliday(any())).thenReturn(false);
        when(attendanceService.save(any(Attendance.class))).thenReturn(true);
        when(attendanceService.updateById(any(Attendance.class))).thenReturn(true);
    }

    @Test
    @DisplayName("工作日 × 2 天 → 生成 2 条休假考勤")
    void approved_Workdays_ShouldCreateLeaveAttendance() {
        // 2026-08-27(周四)、08-28(周五)
        StaffLeave leave = leaveWithDays(2, Date.valueOf("2026-08-27"), LeaveEnum.PERSONAL_LEAVE);
        when(attendanceService.getOne(any(QueryWrapper.class))).thenReturn(null);

        sideEffects.onLeaveApproved(leave);

        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceService, times(2)).save(captor.capture());
        assertEquals(2, captor.getAllValues().size());
        for (Attendance a : captor.getAllValues()) {
            assertEquals(STAFF_ID, a.getStaffId());
            assertEquals(AttendanceStatusEnum.LEAVE, a.getStatus());
        }
    }

    @Test
    @DisplayName("跨周末 → 跳过周末只生成工作日考勤")
    void approved_SpanningWeekend_ShouldSkipWeekend() {
        // 2026-08-28(周五)、08-29(周六)、08-30(周日)、08-31(周一) → 只生成 2 条
        StaffLeave leave = leaveWithDays(4, Date.valueOf("2026-08-28"), LeaveEnum.PERSONAL_LEAVE);
        when(attendanceService.getOne(any(QueryWrapper.class))).thenReturn(null);

        sideEffects.onLeaveApproved(leave);

        verify(attendanceService, times(2)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("遭遇节假日 → 跳过")
    void approved_Holiday_ShouldSkip() {
        // 08-27(周四) 是节假日 → 只处理 08-28(周五)
        StaffLeave leave = leaveWithDays(2, Date.valueOf("2026-08-27"), LeaveEnum.PERSONAL_LEAVE);
        when(datetimeUtil.isHoliday(any())).thenAnswer(inv -> {
            Date d = inv.getArgument(0);
            return d.toString().equals("2026-08-27");
        });
        when(attendanceService.getOne(any(QueryWrapper.class))).thenReturn(null);

        sideEffects.onLeaveApproved(leave);

        verify(attendanceService, times(1)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("调休类型 → 考勤状态调休 + 删除一条调休记录")
    void approved_TimeOff_ShouldSetTimeOffAndDeleteOvertime() {
        StaffLeave leave = leaveWithDays(1, Date.valueOf("2026-08-27"), LeaveEnum.TIME_OFF);
        when(attendanceService.getOne(any(QueryWrapper.class))).thenReturn(null);

        sideEffects.onLeaveApproved(leave);

        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceService).save(captor.capture());
        assertEquals(AttendanceStatusEnum.TIME_OFF, captor.getValue().getStatus());
        verify(staffOvertimeMapper).delete(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("非调休类型 → 不删除调休记录")
    void approved_NotTimeOff_ShouldNotDeleteOvertime() {
        StaffLeave leave = leaveWithDays(1, Date.valueOf("2026-08-27"), LeaveEnum.SICK_LEAVE);
        when(attendanceService.getOne(any(QueryWrapper.class))).thenReturn(null);

        sideEffects.onLeaveApproved(leave);

        verify(staffOvertimeMapper, never()).delete(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("已有考勤记录 → 更新而非新增")
    void approved_ExistingAttendance_ShouldUpdate() {
        StaffLeave leave = leaveWithDays(1, Date.valueOf("2026-08-27"), LeaveEnum.PERSONAL_LEAVE);
        Attendance existing = new Attendance().setId(100).setStaffId(STAFF_ID);
        when(attendanceService.getOne(any(QueryWrapper.class))).thenReturn(existing);
        when(attendanceService.updateById(any(Attendance.class))).thenReturn(true);

        sideEffects.onLeaveApproved(leave);

        verify(attendanceService, never()).save(any(Attendance.class));
        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceService).updateById(captor.capture());
        assertEquals(100, captor.getValue().getId());
    }

    private StaffLeave leaveWithDays(int days, Date startDate, LeaveEnum type) {
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(STAFF_ID);
        leave.setDays(days);
        leave.setStartDate(startDate);
        leave.setTypeNum(type);
        return leave;
    }
}