package com.qiujie.salarycalculation;

import com.qiujie.entity.Salary;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.DeductEnum;
import com.qiujie.vo.StaffSalaryVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 薪资核算纯计算单测——零 mock，只验证规则。
 */
@DisplayName("薪资核算纯计算")
class SalaryCalculationUnitTest {

    private static final Map<Integer, Integer> DEFAULT_RATES = defaultRates();

    private static Map<Integer, Integer> defaultRates() {
        Map<Integer, Integer> rates = new HashMap<>();
        for (DeductEnum type : DeductEnum.values()) {
            rates.put(type.getCode(), type.getDefaultValue());
        }
        return rates;
    }

    private StaffSalaryVO baseVo() {
        StaffSalaryVO vo = new StaffSalaryVO();
        vo.setStaffId(1);
        vo.setDeptId(10);
        vo.setSocialPay(new BigDecimal("500"));
        vo.setHousePay(new BigDecimal("300"));
        return vo;
    }

    private Salary salary() {
        Salary s = new Salary();
        s.setStaffId(1);
        s.setBaseSalary(new BigDecimal("10000"));
        s.setBonus(new BigDecimal("1000"));
        s.setSubsidy(new BigDecimal("500"));
        s.setRemark("ok");
        s.setMonth("202608");
        return s;
    }

    @Test
    @DisplayName("无考勤扣款：总工资 = 10000+1000+500+加班费0 - 社保500 - 公积金300 = 10700")
    void compute_NoDeductions_ShouldSum() {
        StaffSalaryVO vo = baseVo();

        SalaryCalculation.compute(vo, Map.of(), 0, null, salary(), DEFAULT_RATES);

        assertEquals(BigDecimal.ZERO, vo.getLateDeduct());
        assertEquals(BigDecimal.ZERO, vo.getLeaveDeduct());
        assertEquals(0, new BigDecimal("10700").compareTo(vo.getTotalSalary()));
        assertEquals("10000", vo.getBaseSalary().toPlainString());
    }

    @Test
    @DisplayName("迟到 3 次 × 50 + 早退 2 次 × 50 + 旷工 1 次 × 100 + 休假 2 天 × 80 = 550 扣款")
    void compute_DeductionCounts_ShouldMultiply() {
        StaffSalaryVO vo = baseVo();
        Map<Integer, Integer> counts = Map.of(
                AttendanceStatusEnum.LATE.getCode(), 3,
                AttendanceStatusEnum.LEAVE_EARLY.getCode(), 2,
                AttendanceStatusEnum.ABSENTEEISM.getCode(), 1);

        SalaryCalculation.compute(vo, counts, 2, null, salary(), DEFAULT_RATES);

        assertEquals(new BigDecimal("150"), vo.getLateDeduct());
        assertEquals(new BigDecimal("100"), vo.getLeaveEarlyDeduct());
        assertEquals(new BigDecimal("100"), vo.getAbsenteeismDeduct());
        assertEquals(new BigDecimal("160"), vo.getLeaveDeduct());
        // 10000+1000+500 - 510 扣款 - 500 社保 - 300 公积金 = 10190
        assertEquals(0, new BigDecimal("10190").compareTo(vo.getTotalSalary()));
    }

    @Test
    @DisplayName("自定义扣款配置优先于默认")
    void compute_CustomRates_ShouldWin() {
        StaffSalaryVO vo = baseVo();
        Map<Integer, Integer> custom = new HashMap<>(DEFAULT_RATES);
        custom.put(DeductEnum.LATE_DEDUCT.getCode(), 100);
        Map<Integer, Integer> counts = Map.of(AttendanceStatusEnum.LATE.getCode(), 2);

        SalaryCalculation.compute(vo, counts, 0, null, salary(), custom);

        assertEquals(new BigDecimal("200"), vo.getLateDeduct());
    }

    @Test
    @DisplayName("扣款配置缺失回退默认值")
    void compute_MissingRates_ShouldFallbackDefault() {
        StaffSalaryVO vo = baseVo();
        Map<Integer, Integer> counts = Map.of(AttendanceStatusEnum.LATE.getCode(), 1);

        SalaryCalculation.compute(vo, counts, 1, null, salary(), Map.of());

        assertEquals(new BigDecimal("50"), vo.getLateDeduct());
        // 休假 1 个工作日 × 默认 80
        assertEquals(new BigDecimal("80"), vo.getLeaveDeduct());
    }

    @Test
    @DisplayName("加班费入账：10000+1000+500+加班2000 = 13500 - 800 社保公积金 = 12700")
    void compute_WithOvertime_ShouldAdd() {
        StaffSalaryVO vo = baseVo();

        SalaryCalculation.compute(vo, Map.of(), 0, new BigDecimal("2000"), salary(), DEFAULT_RATES);

        assertEquals(new BigDecimal("2000"), vo.getOvertimeSalary());
        assertEquals(0, new BigDecimal("12700").compareTo(vo.getTotalSalary()));
    }

    @Test
    @DisplayName("无当月薪资记录：只填扣款项，不填基础/总工资")
    void compute_NoSalary_ShouldOnlyFillDeductions() {
        StaffSalaryVO vo = baseVo();
        Map<Integer, Integer> counts = Map.of(AttendanceStatusEnum.LATE.getCode(), 1);

        SalaryCalculation.compute(vo, counts, 0, null, null, DEFAULT_RATES);

        assertEquals(new BigDecimal("50"), vo.getLateDeduct());
        assertNull(vo.getBaseSalary());
        assertNull(vo.getTotalSalary());
    }

    @Test
    @DisplayName("社保/公积金为 null 时按 0 处理")
    void compute_NullSocialPay_ShouldTreatAsZero() {
        StaffSalaryVO vo = baseVo();
        vo.setSocialPay(null);
        vo.setHousePay(null);

        SalaryCalculation.compute(vo, Map.of(), 0, null, salary(), DEFAULT_RATES);

        // 10000+1000+500 - 0 - 0 = 11500
        assertEquals(0, new BigDecimal("11500").compareTo(vo.getTotalSalary()));
    }
}