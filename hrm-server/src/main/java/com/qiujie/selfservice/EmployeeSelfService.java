package com.qiujie.selfservice;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.City;
import com.qiujie.entity.Dept;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.LeaveEnum;
import com.qiujie.service.AttendanceService;
import com.qiujie.service.CityService;
import com.qiujie.service.DeptService;
import com.qiujie.service.StaffLeaveService;
import com.qiujie.service.StaffOvertimeService;
import com.qiujie.service.StaffService;
import com.qiujie.util.SecurityUtil;
import com.qiujie.vo.StaffDeptVO;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 员工自助服务门面——AI 助手（{@code ChatTools}）唯一的业务访问点。
 * <p>
 * 隐藏的复杂性：
 * <ul>
 *   <li>6 个服务的注入与拆包——调用方不再接触 ResponseDTO/code 语义</li>
 *   <li>当前员工 staffId 的自动推导——所有查询自动限定"仅本人"</li>
 *   <li>参考数据（部门/城市/请假类型）的取数与空值兜底</li>
 * </ul>
 * 返回领域类型而非 ResponseDTO：null/空列表即"无数据"，不抛异常。
 */
@Component
public class EmployeeSelfService {

    private final StaffLeaveService leaveService;
    private final AttendanceService attendanceService;
    private final StaffOvertimeService overtimeService;
    private final DeptService deptService;
    private final StaffService staffService;
    private final CityService cityService;
    private final SecurityUtil securityUtil;

    public EmployeeSelfService(StaffLeaveService leaveService,
                               AttendanceService attendanceService,
                               StaffOvertimeService overtimeService,
                               DeptService deptService,
                               StaffService staffService,
                               CityService cityService,
                               SecurityUtil securityUtil) {
        this.leaveService = leaveService;
        this.attendanceService = attendanceService;
        this.overtimeService = overtimeService;
        this.deptService = deptService;
        this.staffService = staffService;
        this.cityService = cityService;
        this.securityUtil = securityUtil;
    }

    // ==================== 员工专属查询（自动限定当前员工） ====================

    /**
     * 我的请假记录（默认最近 20 条）。
     * 拆包 {@link StaffLeaveService#queryByStaffId} 的 {pages,total,list} 结构，只返回记录列表。
     */
    public List<Map<String, Object>> myLeaves() {
        ResponseDTO res = leaveService.queryByStaffId(1, 20, currentStaffId());
        if (res == null || res.getData() == null) {
            return Collections.emptyList();
        }
        Object list = ((Map<?, ?>) res.getData()).get("list");
        //noinspection unchecked
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    /**
     * 申请请假。实体构建（staffId 归属 + 类型/日期解析）在此收口；
     * 越权校验沿用 {@link StaffLeaveService#apply} 内部的 staffId 归属检查。
     */
    public ResponseDTO applyLeave(String typeNum, String startDate, int days) {
        // StaffLeave 无 @Accessors(chain)，须分步 set（与 StaffLeaveService.apply 调用方一致）
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(currentStaffId());
        leave.setTypeNum(LeaveEnum.valueOf(typeNum));
        leave.setStartDate(Date.valueOf(startDate));
        leave.setDays(days);
        return leaveService.apply(leave);
    }

    /** 我的考勤（按 yyyyMM）。 */
    public Object myAttendance(String month) {
        ResponseDTO res = attendanceService.queryByStaffIdAndDate(currentStaffId(), month);
        return res != null ? res.getData() : null;
    }

    /** 我的调休余额天数。 */
    public Long myTimeOffBalance() {
        ResponseDTO res = overtimeService.queryTimeOffDaysByStaffId(currentStaffId());
        return res != null && res.getData() instanceof Number
                ? ((Number) res.getData()).longValue() : 0L;
    }

    /** 我的个人信息。 */
    public StaffDeptVO myProfile() {
        ResponseDTO res = staffService.queryInfo(currentStaffId());
        return res != null && res.getData() instanceof StaffDeptVO
                ? (StaffDeptVO) res.getData() : null;
    }

    // ==================== 参考数据（无员工维度） ====================

    /** 可用的请假类型列表。 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> leaveTypes() {
        ResponseDTO res = leaveService.queryAll();
        return res != null && res.getData() instanceof List
                ? (List<Map<String, Object>>) res.getData() : Collections.emptyList();
    }

    /** 公司部门列表。 */
    @SuppressWarnings("unchecked")
    public List<Dept> departments() {
        ResponseDTO res = deptService.queryAll();
        return res != null && res.getData() instanceof List
                ? (List<Dept>) res.getData() : Collections.emptyList();
    }

    /** 公司各城市津贴标准列表。 */
    @SuppressWarnings("unchecked")
    public List<City> cities() {
        ResponseDTO res = cityService.queryAll();
        return res != null && res.getData() instanceof List
                ? (List<City>) res.getData() : Collections.emptyList();
    }

    private Integer currentStaffId() {
        return securityUtil.getCurrentOperatorId();
    }
}