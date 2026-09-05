package com.qiujie.selfservice;

import com.qiujie.dto.Response;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 员工自助服务门面测试：mock 6 个 Service + SecurityUtil，零 Spring 上下文。
 * 验证重点：
 * <ul>
 *   <li>ResponseDTO 拆包——{code,data} → 领域类型</li>
 *   <li>空值兜底——data 为 null / 非预期类型 → 空列表 / 0L / null</li>
 *   <li>staffId 自动注入——所有员工查询都使用 SecurityUtil 的当前用户</li>
 *   <li>applyLeave 实体构建——类型/日期解析 + staffId 归属</li>
 * </ul>
 */
@DisplayName("员工自助服务门面")
class EmployeeSelfServiceUnitTest {

    private static final int STAFF_ID = 9;

    private StaffLeaveService leaveService;
    private AttendanceService attendanceService;
    private StaffOvertimeService overtimeService;
    private DeptService deptService;
    private StaffService staffService;
    private CityService cityService;
    private SecurityUtil securityUtil;
    private EmployeeSelfService selfService;

    @BeforeEach
    void setUp() {
        leaveService = mock(StaffLeaveService.class);
        attendanceService = mock(AttendanceService.class);
        overtimeService = mock(StaffOvertimeService.class);
        deptService = mock(DeptService.class);
        staffService = mock(StaffService.class);
        cityService = mock(CityService.class);
        securityUtil = mock(SecurityUtil.class);
        when(securityUtil.getCurrentOperatorId()).thenReturn(STAFF_ID);

        selfService = new EmployeeSelfService(leaveService, attendanceService, overtimeService,
                deptService, staffService, cityService, securityUtil);
    }

    // ==================== myLeaves ====================

    @Test
    @DisplayName("myLeaves：拆包 {pages,total,list} → 只返回 list")
    void myLeaves_ShouldUnwrapListFromPaginatedData() {
        List<Map<String, Object>> expected = List.of(new HashMap<>());
        Map<String, Object> data = new HashMap<>();
        data.put("pages", 1);
        data.put("total", 1L);
        data.put("list", expected);
        when(leaveService.queryByStaffId(1, 20, STAFF_ID)).thenReturn(Response.success(data));

        List<Map<String, Object>> result = selfService.myLeaves();

        assertEquals(expected, result);
        // staffId 自动注入当前员工
        verify(leaveService).queryByStaffId(1, 20, STAFF_ID);
    }

    @Test
    @DisplayName("myLeaves：data 为 null → 空列表")
    void myLeaves_NullData_ShouldReturnEmpty() {
        when(leaveService.queryByStaffId(1, 20, STAFF_ID)).thenReturn(Response.success(null));

        assertEquals(Collections.emptyList(), selfService.myLeaves());
    }

    @Test
    @DisplayName("myLeaves：响应为 null → 空列表")
    void myLeaves_NullResponse_ShouldReturnEmpty() {
        when(leaveService.queryByStaffId(1, 20, STAFF_ID)).thenReturn(null);

        assertEquals(Collections.emptyList(), selfService.myLeaves());
    }

    @Test
    @DisplayName("myLeaves：data 非 Map 或 list 缺失 → 空列表")
    void myLeaves_NonMapData_ShouldReturnEmpty() {
        when(leaveService.queryByStaffId(1, 20, STAFF_ID)).thenReturn(Response.success("not-a-map"));

        assertEquals(Collections.emptyList(), selfService.myLeaves());
    }

    // ==================== myAttendance ====================

    @Test
    @DisplayName("myAttendance：正常返回 data")
    void myAttendance_ShouldReturnData() {
        Object attendance = Map.of("attendanceDate", "2026-06-01");
        when(attendanceService.queryByStaffIdAndDate(STAFF_ID, "202606"))
                .thenReturn(Response.success(attendance));

        assertEquals(attendance, selfService.myAttendance("202606"));
        verify(attendanceService).queryByStaffIdAndDate(STAFF_ID, "202606");
    }

    @Test
    @DisplayName("myAttendance：无记录 → null")
    void myAttendance_NoRecord_ShouldReturnNull() {
        when(attendanceService.queryByStaffIdAndDate(STAFF_ID, "202606"))
                .thenReturn(Response.error());

        assertNull(selfService.myAttendance("202606"));
    }

    // ==================== myTimeOffBalance ====================

    @Test
    @DisplayName("myTimeOffBalance：正常返回 Long")
    void myTimeOffBalance_ShouldReturnLong() {
        when(overtimeService.queryTimeOffDaysByStaffId(STAFF_ID))
                .thenReturn(Response.success(3L));

        assertEquals(3L, selfService.myTimeOffBalance());
    }

    @Test
    @DisplayName("myTimeOffBalance：无记录 → 0L")
    void myTimeOffBalance_NoRecord_ShouldReturnZero() {
        when(overtimeService.queryTimeOffDaysByStaffId(STAFF_ID))
                .thenReturn(Response.success(null));

        assertEquals(0L, selfService.myTimeOffBalance());
    }

    // ==================== myProfile ====================

    @Test
    @DisplayName("myProfile：正常返回 StaffDeptVO")
    void myProfile_ShouldReturnStaffDeptVO() {
        StaffDeptVO vo = new StaffDeptVO();
        when(staffService.queryInfo(STAFF_ID)).thenReturn(Response.success(vo));

        assertEquals(vo, selfService.myProfile());
    }

    @Test
    @DisplayName("myProfile：无记录 → null")
    void myProfile_NoRecord_ShouldReturnNull() {
        when(staffService.queryInfo(STAFF_ID)).thenReturn(Response.error());

        assertNull(selfService.myProfile());
    }

    // ==================== 参考数据 ====================

    @Test
    @DisplayName("leaveTypes / departments / cities：正常返回 list")
    void referenceData_ShouldReturnList() {
        when(leaveService.queryAll()).thenReturn(Response.success(List.of(Map.of("code", 0))));
        Dept dept = new Dept();
        when(deptService.queryAll()).thenReturn(Response.success(List.of(dept)));
        City city = new City();
        when(cityService.queryAll()).thenReturn(Response.success(List.of(city)));

        assertEquals(1, selfService.leaveTypes().size());
        assertEquals(List.of(dept), selfService.departments());
        assertEquals(List.of(city), selfService.cities());
    }

    @Test
    @DisplayName("leaveTypes：data 为 null → 空列表")
    void leaveTypes_NullData_ShouldReturnEmpty() {
        when(leaveService.queryAll()).thenReturn(Response.success(null));

        assertEquals(Collections.emptyList(), selfService.leaveTypes());
    }

    @Test
    @DisplayName("departments / cities：data 非 List → 空列表")
    void referenceData_NonListData_ShouldReturnEmpty() {
        when(deptService.queryAll()).thenReturn(Response.success("not-list"));
        when(cityService.queryAll()).thenReturn(Response.success(42));

        assertEquals(Collections.emptyList(), selfService.departments());
        assertEquals(Collections.emptyList(), selfService.cities());
    }

    // ==================== applyLeave ====================

    @Test
    @DisplayName("applyLeave：构建实体并委托 Service")
    void applyLeave_ShouldBuildEntityAndDelegate() {
        when(leaveService.apply(any(StaffLeave.class))).thenAnswer(inv -> {
            StaffLeave leave = inv.getArgument(0);
            return Response.success(leave);
        });

        ResponseDTO result = selfService.applyLeave("PERSONAL_LEAVE", "2026-09-01", 3);

        assertEquals(200, result.getCode());
        org.mockito.ArgumentCaptor<StaffLeave> captor = org.mockito.ArgumentCaptor.forClass(StaffLeave.class);
        verify(leaveService).apply(captor.capture());
        StaffLeave leave = captor.getValue();
        // staffId 归属收口在门面内
        assertEquals(STAFF_ID, leave.getStaffId());
        // 类型/日期解析
        assertEquals(LeaveEnum.PERSONAL_LEAVE, leave.getTypeNum());
        assertEquals(Date.valueOf("2026-09-01"), leave.getStartDate());
        assertEquals(3, leave.getDays());
    }

    @Test
    @DisplayName("applyLeave：直接透传 Service 响应（成功/失败）")
    void applyLeave_ShouldPassThroughServiceResponse() {
        when(leaveService.apply(any(StaffLeave.class)))
                .thenReturn(Response.success("已提交"));

        ResponseDTO result = selfService.applyLeave("SICK_LEAVE", "2026-09-02", 1);

        assertEquals(200, result.getCode());
        assertEquals("已提交", result.getMessage());
    }

    @Test
    @DisplayName("applyLeave：非法类型抛 IllegalArgumentException")
    void applyLeave_InvalidType_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> selfService.applyLeave("NOT_A_TYPE", "2026-09-01", 3));
        verify(leaveService, never()).apply(any());
    }

    @Test
    @DisplayName("applyLeave：非法日期抛 IllegalArgumentException")
    void applyLeave_InvalidDate_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> selfService.applyLeave("PERSONAL_LEAVE", "not-a-date", 3));
        verify(leaveService, never()).apply(any());
    }
}