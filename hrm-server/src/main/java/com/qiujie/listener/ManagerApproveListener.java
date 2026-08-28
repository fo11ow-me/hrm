package com.qiujie.listener;

import com.qiujie.entity.StaffLeave;
import com.qiujie.leaveapproval.LeaveApprovalSideEffects;
import com.qiujie.service.StaffLeaveService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * 经理审批执行监听器——流程转译层。
 * <p>
 * 考勤/调休同步的副作用委托给 {@link LeaveApprovalSideEffects}，本类只做：
 * 按流程实例业务 key 取请假记录 → 触发审批副作用。
 * </p>
 */
@Component
public class ManagerApproveListener implements ExecutionListener {

    private final StaffLeaveService staffLeaveService;
    private final LeaveApprovalSideEffects sideEffects;

    public ManagerApproveListener(StaffLeaveService staffLeaveService,
                                  LeaveApprovalSideEffects sideEffects) {
        this.staffLeaveService = staffLeaveService;
        this.sideEffects = sideEffects;
    }

    @Override
    public void notify(DelegateExecution execution) {
        StaffLeave staffLeave = staffLeaveService.getOne(new QueryWrapper<StaffLeave>()
                .eq("id", Integer.valueOf(execution.getProcessInstanceBusinessKey())));
        sideEffects.onLeaveApproved(staffLeave);
    }
}