package com.qiujie.leaveapproval;

import com.qiujie.entity.StaffLeave;

/**
 * 请假通知端口——审批流程各节点对申请人/审批人的 SSE 通知。
 * <p>
 * 由 {@code StaffLeaveService} 与 Flowable 监听器调用，隐藏通知事件构造与
 * 接收人解析，使业务层不直接依赖 {@code SseService}。
 * </p>
 */
public interface LeaveNotifier {

    /**
     * 请假申请已提交：通知申请人"已提交"，通知 HR 有新的待审申请。
     *
     * @param leave    已提交的请假
     * @param hrIds    待审 HR 员工 id 列表
     */
    void onLeaveSubmitted(StaffLeave leave, java.util.List<Integer> hrIds);

    /**
     * 请假审批完成（通过/拒绝）：通知申请人结果。
     *
     * @param leave 已完成的请假（status 为 APPROVE 或 REJECT）
     */
    void onLeaveCompleted(StaffLeave leave);
}