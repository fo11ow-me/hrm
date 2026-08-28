package com.qiujie.leaveapproval;

import com.qiujie.entity.Staff;
import com.qiujie.mapper.StaffMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批候选人解析——为 Flowable 流程变量提供角色成员工号集合。
 * <p>
 * 收敛 {@code HrApproveListener} 中的角色查询与流程变量装配，使监听器保持流程转译。
 * </p>
 */
@Component
public class ApprovalCandidateResolver {

    private final StaffMapper staffMapper;

    public ApprovalCandidateResolver(StaffMapper staffMapper) {
        this.staffMapper = staffMapper;
    }

    /**
     * 查询指定角色的员工工号，按逗号拼接（Flowable 候选人变量格式）。
     *
     * @param role 角色 code（如 manager）
     * @return 工号逗号串；无成员时返回空串
     */
    public String codesOfRole(String role) {
        List<Staff> staffList = staffMapper.queryByRole(role);
        return staffList.stream().map(Staff::getCode).collect(Collectors.joining(","));
    }
}