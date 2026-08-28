package com.qiujie.leaveapproval;

import com.qiujie.entity.StaffLeave;

/**
 * 请假审批副作用端口——审批通过后对考勤/调休的同步动作。
 * <p>
 * 由 Flowable 监听器调用，隐藏全部副作用规则（逐日遍历、周末/节假日过滤、
 * 考勤落库、调休扣减），使监听器保持流程转译而非业务实现。
 * </p>
 */
public interface LeaveApprovalSideEffects {

    /**
     * 请假通过后的副作用：
     * <ul>
     *   <li>逐日遍历请假区间（跳过周末/节假日），为每个工作日生成/更新考勤记录</li>
     *   <li>{@code TIME_OFF} 类型：考勤状态为调休，并扣减该员工一条调休记录</li>
     *   <li>其他类型：考勤状态为休假</li>
     * </ul>
     *
     * @param leave 已审批通过的请假记录
     */
    void onLeaveApproved(StaffLeave leave);
}