package com.qiujie.overtime;

import com.qiujie.entity.Overtime;
import com.qiujie.entity.Salary;
import com.qiujie.entity.Staff;
import com.qiujie.entity.StaffOvertime;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import com.qiujie.mapper.OvertimeMapper;
import com.qiujie.mapper.SalaryMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.DatetimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OvertimeCalculator 单元测试——纯计算规则 + 引用数据加载兜底。
 * <p>
 * 计算规则通过 {@link OvertimeCalculator#computeFromReferences}（纯静态，零 mock）验证；
 * 加载逻辑通过 mock Mapper 验证。
 * </p>
 */
@DisplayName("OvertimeCalculator 加班计算深模块")
class OvertimeCalculatorUnitTest {

    private static final int STAFF_ID = 1;
    private static final int DEPT_ID = 10;

    private StaffMapper staffMapper;
    private OvertimeMapper overtimeMapper;
    private SalaryMapper salaryMapper;
    private DatetimeUtil datetimeUtil;
    private OvertimeCalculator calculator;

    private Staff staff;
    private Overtime dayOffConfig;
    private Salary salary;

    @BeforeEach
    void setUp() {
        staffMapper = mock(StaffMapper.class);
        overtimeMapper = mock(OvertimeMapper.class);
        salaryMapper = mock(SalaryMapper.class);
        datetimeUtil = mock(DatetimeUtil.class);
        calculator = new OvertimeCalculator(staffMapper, overtimeMapper, salaryMapper, datetimeUtil);

        staff = new Staff();
        staff.setId(STAFF_ID);
        staff.setDeptId(DEPT_ID);

        dayOffConfig = new Overtime();
        dayOffConfig.setTypeNum(OvertimeEnum.DAY_OFF_OVERTIME);
        dayOffConfig.setDeptId(DEPT_ID);
        dayOffConfig.setSalaryMultiple(new BigDecimal("2.0"));
        dayOffConfig.setBonus(BigDecimal.ZERO);
        dayOffConfig.setCountType(0);
        dayOffConfig.setTimeOffFlag(0);

        salary = new Salary();
        salary.setStaffId(STAFF_ID);
        salary.setHourSalary(new BigDecimal("100"));
        salary.setDaySalary(new BigDecimal("800"));
        salary.setMonth("202608");
    }

    // ==================== 纯计算：时长 ====================

    @Test
    @DisplayName("时长：上午 2h + 下午 3h = 5h")
    void totalOvertime_ShouldSumMorningAndAfternoon() {
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00");

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, dayOffConfig, salary, NEVER_HOLIDAY);

        assertEquals(new BigDecimal("5.0"), r.totalOvertime());
    }

    // ==================== 纯计算：日期分类 ====================

    @Test
    @DisplayName("日期分类：节假日 → HOLIDAY")
    void classify_Holiday_ShouldBeHolidayOvertime() {
        StaffOvertime src = srcWithTypeNum(null, "2026-08-28");

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, null, null, null, d -> true);

        assertEquals(OvertimeEnum.HOLIDAY_OVERTIME, r.typeNum());
    }

    @Test
    @DisplayName("日期分类：周末 → DAY_OFF")
    void classify_Weekend_ShouldBeDayOff() {
        StaffOvertime src = srcWithTypeNum(null, "2026-08-29"); // 周六

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, null, null, null, NEVER_HOLIDAY);

        assertEquals(OvertimeEnum.DAY_OFF_OVERTIME, r.typeNum());
    }

    @Test
    @DisplayName("日期分类：工作日 → WORKDAY")
    void classify_Workday_ShouldBeWorkdayOvertime() {
        StaffOvertime src = srcWithTypeNum(null, "2026-08-28"); // 周五

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, null, null, null, NEVER_HOLIDAY);

        assertEquals(OvertimeEnum.WORKDAY_OVERTIME, r.typeNum());
    }

    @Test
    @DisplayName("已有类型：信任 src 传入的类型（手动指定）")
    void classify_ExplicitType_ShouldTrustInput() {
        StaffOvertime src = srcWithTypeNum(OvertimeEnum.DAY_OFF_OVERTIME, "2026-08-28");

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, null, null, null, NEVER_HOLIDAY);

        assertEquals(OvertimeEnum.DAY_OFF_OVERTIME, r.typeNum());
    }

    // ==================== 纯计算：调休判定 ====================

    @Test
    @DisplayName("调休：休息日 + timeOffFlag=1 + 时长≥8h → TIME_OFF 不计薪")
    void timeOff_Qualified_ShouldBeTimeOff() {
        Overtime config = new Overtime();
        config.setTimeOffFlag(1);
        StaffOvertime src = srcWithTimes("08:00:00", "12:00:00", "13:00:00", "17:30:00"); // 8.5h
        src.setTypeNum(OvertimeEnum.DAY_OFF_OVERTIME);

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, config, salary, NEVER_HOLIDAY);

        assertEquals(OvertimeStatusEnum.TIME_OFF, r.status());
        assertNull(r.overtimeSalary());
    }

    @Test
    @DisplayName("调休：时长不足 8h → 仍按加班计薪")
    void timeOff_NotEnoughHours_ShouldPay() {
        Overtime config = new Overtime();
        config.setTypeNum(OvertimeEnum.DAY_OFF_OVERTIME);
        config.setSalaryMultiple(new BigDecimal("2.0"));
        config.setBonus(BigDecimal.ZERO);
        config.setCountType(0);
        config.setTimeOffFlag(1);
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00"); // 5h
        src.setTypeNum(OvertimeEnum.DAY_OFF_OVERTIME);

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, config, salary, NEVER_HOLIDAY);

        assertEquals(OvertimeStatusEnum.OVERTIME, r.status());
        assertNotNull(r.overtimeSalary());
    }

    // ==================== 纯计算：计薪规则 ====================

    @Test
    @DisplayName("计薪：按小时 5h × 时薪100 × 倍数2 = 1000")
    void salary_HourBased_ShouldMultiply() {
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00"); // 5h

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, dayOffConfig, salary, NEVER_HOLIDAY);

        assertEquals(0, new BigDecimal("1000").compareTo(r.overtimeSalary()));
    }

    @Test
    @DisplayName("计薪：按小时不足 2h → 0")
    void salary_HourBased_BelowThreshold_ShouldBeZero() {
        StaffOvertime src = srcWithTimes("09:00:00", "09:30:00", "13:00:00", "13:30:00"); // 1h

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, dayOffConfig, salary, NEVER_HOLIDAY);

        assertEquals(BigDecimal.ZERO, r.overtimeSalary());
    }

    @Test
    @DisplayName("计薪：按天 8h 日薪800 × 倍数1.5 = 1200")
    void salary_DayBased_ShouldMultiply() {
        Overtime config = new Overtime();
        config.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        config.setSalaryMultiple(new BigDecimal("1.5"));
        config.setBonus(BigDecimal.ZERO);
        config.setCountType(1);
        config.setTimeOffFlag(0);
        StaffOvertime src = srcWithTimes("08:00:00", "12:00:00", "13:00:00", "17:00:00"); // 8h

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, config, salary, NEVER_HOLIDAY);

        assertEquals(0, new BigDecimal("1200").compareTo(r.overtimeSalary()));
    }

    @Test
    @DisplayName("计薪：按天不足 8h → 0")
    void salary_DayBased_BelowThreshold_ShouldBeZero() {
        Overtime config = new Overtime();
        config.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        config.setSalaryMultiple(new BigDecimal("1.5"));
        config.setBonus(BigDecimal.ZERO);
        config.setCountType(1);
        config.setTimeOffFlag(0);
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00"); // 5h

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, config, salary, NEVER_HOLIDAY);

        assertEquals(BigDecimal.ZERO, r.overtimeSalary());
    }

    // ==================== 兜底 ====================

    @Test
    @DisplayName("兜底：薪资记录缺失 → 不计薪但保留加班状态")
    void fallback_NoSalary_ShouldNotPayButKeepOvertime() {
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00");

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, dayOffConfig, null, NEVER_HOLIDAY);

        assertNull(r.overtimeSalary());
        assertEquals(OvertimeStatusEnum.OVERTIME, r.status());
    }

    @Test
    @DisplayName("兜底：配置缺失 → 不计薪")
    void fallback_NoConfig_ShouldNotPay() {
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00");

        OvertimeResult r = OvertimeCalculator.computeFromReferences(src, staff, null, salary, NEVER_HOLIDAY);

        assertNull(r.overtimeSalary());
    }

    @Test
    @DisplayName("兜底：输入为空 → 空结果不崩")
    void fallback_NullInput_ShouldNotThrow() {
        OvertimeResult r = OvertimeCalculator.computeFromReferences(null, null, null, null, NEVER_HOLIDAY);
        assertNotNull(r);
        assertEquals(BigDecimal.ZERO, r.totalOvertime());
    }

    // ==================== 批量入口：引用缓存 ====================

    @Test
    @DisplayName("批量入口：命中缓存不重复查询")
    void compute_WithCache_ShouldNotQuery() {
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00");
        Map<Integer, Staff> staffCache = Map.of(STAFF_ID, staff);
        Map<Integer, Salary> salaryCache = Map.of(STAFF_ID, salary);

        calculator.compute(STAFF_ID, src, staffCache, null, salaryCache);

        verify(staffMapper, never()).selectById(any());
        verify(salaryMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("批量入口：缓存缺失回退 DB 查询")
    void compute_MissingCache_ShouldQueryDb() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(staff);
        when(datetimeUtil.isHoliday(any())).thenReturn(false);
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00");

        calculator.compute(STAFF_ID, src, null, null, null);

        verify(staffMapper).selectById(STAFF_ID);
    }

    @Test
    @DisplayName("批量入口：员工不存在 → 空结果不崩")
    void compute_StaffNotFound_ShouldReturnEmpty() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(null);
        StaffOvertime src = srcWithTimes("09:00:00", "11:00:00", "13:00:00", "16:00:00");

        OvertimeResult r = calculator.compute(STAFF_ID, src, null, null, null);

        assertNull(r.typeNum());
        assertEquals(OvertimeStatusEnum.NORMAL, r.status());
    }

    // ==================== 工具 ====================

    private static final Predicate<Date> NEVER_HOLIDAY = d -> false;

    private StaffOvertime srcWithTimes(String morStart, String morEnd, String aftStart, String aftEnd) {
        return new StaffOvertime()
                .setStaffId(STAFF_ID)
                .setMorStartTime(Timestamp.valueOf("2026-08-28 " + morStart))
                .setMorEndTime(Timestamp.valueOf("2026-08-28 " + morEnd))
                .setAftStartTime(Timestamp.valueOf("2026-08-28 " + aftStart))
                .setAftEndTime(Timestamp.valueOf("2026-08-28 " + aftEnd))
                .setOvertimeDate(Date.valueOf("2026-08-28"));
    }

    private StaffOvertime srcWithTypeNum(OvertimeEnum typeNum, String date) {
        return new StaffOvertime()
                .setStaffId(STAFF_ID)
                .setTypeNum(typeNum)
                .setOvertimeDate(Date.valueOf(date));
    }
}