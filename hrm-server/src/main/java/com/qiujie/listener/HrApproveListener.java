package com.qiujie.listener;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.qiujie.entity.Staff;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.AuditStatusEnum;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.service.StaffLeaveService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HrApproveListener implements ExecutionListener {

    @Autowired
    private StaffLeaveService staffLeaveService;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Override
    @Transactional
    public void notify(DelegateExecution execution) {
        UpdateWrapper<StaffLeave> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", AuditStatusEnum.UNAUDITED).eq("id", Integer.valueOf(execution.getProcessInstanceBusinessKey()));
        if (!this.staffLeaveService.update(updateWrapper)) {
            throw new ServiceException(BusinessStatusEnum.ERROR);
        }
        List<Staff> staffList = this.staffMapper.queryByRole("manager");
        Map<String, Object> variables = new HashMap<>();
        variables.put("manager", staffList.stream().map(Staff::getCode).collect(Collectors.joining(",")));
        runtimeService.setVariables(execution.getId(), variables);
    }
}
