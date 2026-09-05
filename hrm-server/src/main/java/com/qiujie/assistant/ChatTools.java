package com.qiujie.assistant;

import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.selfservice.EmployeeSelfService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * AI 助手工具集——每个 @Tool 方法暴露一个 assistant 可调用的能力。
 * <p>
 * 所有业务逻辑委托给 {@link EmployeeSelfService} 门面，不直接接触 Controller 或 Service。
 * 门面负责 staffId 注入、ResponseDTO 拆包、空值兜底，本层仅做类型适配与 JSON 序列化。
 */
@Component
public class ChatTools {

    private final EmployeeSelfService selfService;

    public ChatTools(EmployeeSelfService selfService) {
        this.selfService = selfService;
    }

    @Tool(description = "查询我的请假记录列表")
    public ResponseDTO myLeaves() {
        return Response.success(selfService.myLeaves());
    }

    @Tool(description = "查询我的考勤。month 格式 yyyyMM，例如 202606")
    public ResponseDTO myAttendance(
            @ToolParam(description = "月份，格式 yyyyMM") String month) {
        Object data = selfService.myAttendance(month);
        return data != null ? Response.success(data) : Response.error("暂无考勤记录");
    }

    @Tool(description = "查询我的调休余额天数")
    public ResponseDTO myTimeOffBalance() {
        return Response.success(selfService.myTimeOffBalance());
    }

    @Tool(description = "查询可用的请假类型列表")
    public ResponseDTO leaveTypes() {
        return Response.success(selfService.leaveTypes());
    }

    @Tool(description = "查询公司部门列表")
    public ResponseDTO departments() {
        return Response.success(selfService.departments());
    }

    @Tool(description = "查询我的个人信息")
    public ResponseDTO myProfile() {
        return Response.success(selfService.myProfile());
    }

    @Tool(description = "查询公司各城市的津贴标准列表")
    public ResponseDTO cities() {
        return Response.success(selfService.cities());
    }

    @Tool(description = "申请请假。typeNum 可选：事假/病假/婚假/产假/陪产假/探亲假/调休，startDate 格式 yyyy-MM-dd")
    public ResponseDTO applyLeave(
            @ToolParam(description = "请假类型") String typeNum,
            @ToolParam(description = "开始日期 yyyy-MM-dd") String startDate,
            @ToolParam(description = "天数") int days) {
        return selfService.applyLeave(typeNum, startDate, days);
    }
}