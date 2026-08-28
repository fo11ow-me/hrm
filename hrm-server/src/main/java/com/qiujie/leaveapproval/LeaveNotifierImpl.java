package com.qiujie.leaveapproval;

import com.qiujie.dto.notification.NotificationEvent;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.AuditStatusEnum;
import com.qiujie.service.SseService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 请假通知生产实现——收敛 {@code StaffLeaveService} 中的 SSE 通知构造。
 * <p>
 * 事件类型：提交时通知申请人({@code LEAVE_SUBMITTED}) + 通知 HR({@code LEAVE_PENDING})；
 * 完成时按结果通知申请人({@code LEAVE_APPROVED} / {@code LEAVE_REJECTED})。
 * 通知失败仅告警，不阻断主流程。
 * </p>
 */
@Component
public class LeaveNotifierImpl implements LeaveNotifier {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LeaveNotifierImpl.class);

    private final SseService sseService;

    public LeaveNotifierImpl(SseService sseService) {
        this.sseService = sseService;
    }

    @Override
    public void onLeaveSubmitted(StaffLeave leave, List<Integer> hrIds) {
        try {
            String desc = leave.getDays() + "天请假，等待审批";
            sseService.emit(leave.getStaffId(), "notification",
                    new NotificationEvent("LEAVE_SUBMITTED", "请假申请已提交", desc));
            if (hrIds != null) {
                for (Integer hrId : hrIds) {
                    sseService.emit(hrId, "notification",
                            new NotificationEvent("LEAVE_PENDING", "新的请假申请", "员工提交了" + leave.getDays() + "天请假"));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to push leave submitted notification", e);
        }
    }

    @Override
    public void onLeaveCompleted(StaffLeave leave) {
        try {
            boolean approved = AuditStatusEnum.APPROVE.equals(leave.getStatus());
            String type = approved ? "LEAVE_APPROVED" : "LEAVE_REJECTED";
            String title = "请假" + (approved ? "已通过" : "已拒绝");
            String body = leave.getDays() + "天请假" + (approved ? "已通过" : "已拒绝");
            sseService.emit(leave.getStaffId(), "notification", new NotificationEvent(type, title, body));
        } catch (Exception e) {
            log.warn("Failed to push leave completed notification", e);
        }
    }
}