package com.qiujie.leaveapproval;

import com.qiujie.dto.notification.NotificationEvent;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.AuditStatusEnum;
import com.qiujie.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 请假通知单测——零 Spring 上下文，mock SseService。
 */
@DisplayName("请假通知")
class LeaveNotifierUnitTest {

    private static final int STAFF_ID = 9;

    private SseService sseService;
    private LeaveNotifier notifier;

    @BeforeEach
    void setUp() {
        sseService = mock(SseService.class);
        notifier = new LeaveNotifierImpl(sseService);
    }

    @Test
    @DisplayName("提交通知：申请人 + 每个 HR")
    void submitted_ShouldNotifyApplicantAndHr() {
        StaffLeave leave = leaveWithDays(2);
        List<Integer> hrIds = List.of(1, 2);

        notifier.onLeaveSubmitted(leave, hrIds);

        verify(sseService, times(3)).emit(any(), eq("notification"), any(NotificationEvent.class));
        ArgumentCaptor<Integer> userIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(sseService, times(3)).emit(userIdCaptor.capture(), eq("notification"), any(NotificationEvent.class));
        assertEquals(List.of(STAFF_ID, 1, 2), userIdCaptor.getAllValues());
    }

    @Test
    @DisplayName("提交通知：HR 为空就不通知")
    void submitted_NoHr_ShouldOnlyNotifyApplicant() {
        StaffLeave leave = leaveWithDays(2);

        notifier.onLeaveSubmitted(leave, null);

        verify(sseService, times(1)).emit(any(), eq("notification"), any(NotificationEvent.class));
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(sseService).emit(eq(STAFF_ID), eq("notification"), captor.capture());
        assertEquals("LEAVE_SUBMITTED", captor.getValue().getType());
    }

    @Test
    @DisplayName("通过通知：LEAVE_APPROVED")
    void completed_Approved_ShouldSendApprovedEvent() {
        StaffLeave leave = leaveWithDays(3);
        leave.setStatus(AuditStatusEnum.APPROVE);

        notifier.onLeaveCompleted(leave);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(sseService).emit(eq(STAFF_ID), eq("notification"), captor.capture());
        assertEquals("LEAVE_APPROVED", captor.getValue().getType());
    }

    @Test
    @DisplayName("拒绝通知：LEAVE_REJECTED")
    void completed_Rejected_ShouldSendRejectedEvent() {
        StaffLeave leave = leaveWithDays(3);
        leave.setStatus(AuditStatusEnum.REJECT);

        notifier.onLeaveCompleted(leave);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(sseService).emit(eq(STAFF_ID), eq("notification"), captor.capture());
        assertEquals("LEAVE_REJECTED", captor.getValue().getType());
    }

    private StaffLeave leaveWithDays(int days) {
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(STAFF_ID);
        leave.setDays(days);
        return leave;
    }
}