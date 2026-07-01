package com.qiujie.assistant;

import com.qiujie.controller.*;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.StaffLeave;
import com.qiujie.util.SecurityUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI 助手工具集——每个 @Tool 方法暴露一个 assistant 可调用的能力。
 * 权限校验由 Controller 层的 @PreAuthorize 和 Service 逻辑保障，Tool 本身不做鉴权。
 */
@Component
public class ChatTools {

    @Autowired private StaffLeaveController leaveCtrl;
    @Autowired private AttendanceController attCtrl;
    @Autowired private StaffOvertimeController otCtrl;
    @Autowired private DeptController deptCtrl;
    @Autowired private StaffController staffCtrl;
    @Autowired private SecurityUtil securityUtil;

    private Integer staffId() { return securityUtil.getCurrentOperatorId(); }

    @Tool(description = "查询我的请假记录列表")
    public ResponseDTO myLeaves() {
        return leaveCtrl.queryByStaffId(1, 20, staffId());
    }

    @Tool(description = "申请请假。typeNum 可选：事假/病假/婚假/产假/陪产假/探亲假/调休，startDate 格式 yyyy-MM-dd")
    public ResponseDTO applyLeave(
            @ToolParam(description = "请假类型") String typeNum,
            @ToolParam(description = "开始日期 yyyy-MM-dd") String startDate,
            @ToolParam(description = "天数") int days) {
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(staffId());
        leave.setTypeNum(com.qiujie.enums.LeaveEnum.valueOf(typeNum));
        leave.setStartDate(java.sql.Date.valueOf(startDate));
        leave.setDays(days);
        return leaveCtrl.apply(leave);
    }

    @Tool(description = "查询我某月的考勤。month 格式 yyyyMM，例如 202606")
    public ResponseDTO myAttendance(
            @ToolParam(description = "月份，格式 yyyyMM") String month) {
        return attCtrl.queryByStaffIdAndDate(staffId(), month);
    }

    @Tool(description = "查询我的调休余额天数")
    public ResponseDTO myTimeOffBalance() {
        return otCtrl.queryTimeOffDaysByStaffId(staffId());
    }

    @Tool(description = "查询可用的请假类型列表")
    public ResponseDTO leaveTypes() {
        return leaveCtrl.queryAll();
    }

    @Tool(description = "查询公司部门列表")
    public ResponseDTO departments() {
        return deptCtrl.queryAll();
    }

    @Tool(description = "查询我的个人信息")
    public ResponseDTO myProfile() {
        return staffCtrl.queryInfo(staffId());
    }
}
