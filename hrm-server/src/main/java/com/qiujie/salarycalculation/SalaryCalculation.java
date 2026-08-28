package com.qiujie.salarycalculation;

import com.qiujie.entity.Salary;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.DeductEnum;
import com.qiujie.vo.StaffSalaryVO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 薪资核算纯计算——从 {@code SalaryService.setSalaryInfo} 提取的不可变计算规则。
 * <p>
 * 输入为解析好的引用数据，无任何 I/O，可直接零 mock 单测。隐藏的复杂性：
 * <ul>
 *   <li>迟到/早退/旷工/休假扣款金额 × 次数 → 扣款小计（按 {@link DeductEnum} 配置，缺失回退默认值）</li>
 *   <li>休假扣款计数已由调用方扣除周末（本模块只做乘法）</li>
 *   <li>总工资 = 基础 + 奖金 + 补贴 + 加班费 − 各项扣款 − 社保 − 公积金</li>
 * </ul>
 * </p>
 */
public final class SalaryCalculation {

    private SalaryCalculation() {
    }

    /**
     * 计算单个员工某月的薪资明细。
     *
     * @param vo               员工薪资视图（需已含 deptId/staffId、socialPay/housePay）
     * @param attendanceCounts 考勤次数：{@link AttendanceStatusEnum} code → 当月次数（可 null）
     * @param leaveWorkdayCount 休假天数中扣除周末后的工作日数
     * @param monthOvertime    当月加班费汇总（可 null → 0，仅在有薪资记录时入账）
     * @param monthSalary      当月薪资记录（可 null → 只填扣款项，不填入账项）
     * @param deductRates      扣款配置：{@link DeductEnum} code → 金额（缺失回退 {@link DeductEnum#getDefaultValue()}）
     * @return 已填充的 vo
     */
    public static StaffSalaryVO compute(StaffSalaryVO vo,
                                        Map<Integer, Integer> attendanceCounts,
                                        int leaveWorkdayCount,
                                        BigDecimal monthOvertime,
                                        Salary monthSalary,
                                        Map<Integer, Integer> deductRates) {
        BigDecimal lateDeduct = deduct(vo, AttendanceStatusEnum.LATE, attendanceCounts, deductRates);
        BigDecimal earlyDeduct = deduct(vo, AttendanceStatusEnum.LEAVE_EARLY, attendanceCounts, deductRates);
        BigDecimal absenteeismDeduct = deduct(vo, AttendanceStatusEnum.ABSENTEEISM, attendanceCounts, deductRates);
        BigDecimal leaveDeduct = BigDecimal.valueOf(leaveWorkdayCount)
                .multiply(BigDecimal.valueOf(rateOf(DeductEnum.LEAVE_DEDUCT, deductRates)));

        vo.setLateDeduct(lateDeduct)
                .setLeaveEarlyDeduct(earlyDeduct)
                .setAbsenteeismDeduct(absenteeismDeduct)
                .setLeaveDeduct(leaveDeduct);

        if (monthSalary != null) {
            BigDecimal overtime = monthOvertime == null ? BigDecimal.ZERO : monthOvertime;
            BigDecimal total = monthSalary.getBaseSalary()
                    .add(monthSalary.getBonus())
                    .add(monthSalary.getSubsidy())
                    .add(overtime)
                    .subtract(lateDeduct)
                    .subtract(earlyDeduct)
                    .subtract(absenteeismDeduct)
                    .subtract(leaveDeduct)
                    .subtract(nullToZero(vo.getSocialPay()))
                    .subtract(nullToZero(vo.getHousePay()));
            vo.setBaseSalary(monthSalary.getBaseSalary())
                    .setOvertimeSalary(overtime)
                    .setSubsidy(monthSalary.getSubsidy())
                    .setBonus(monthSalary.getBonus())
                    .setRemark(monthSalary.getRemark())
                    .setTotalSalary(total);
        }
        return vo;
    }

    private static BigDecimal deduct(StaffSalaryVO vo, AttendanceStatusEnum status,
                                     Map<Integer, Integer> counts, Map<Integer, Integer> deductRates) {
        int count = counts == null ? 0 : counts.getOrDefault(status.getCode(), 0);
        int rate = rateOf(toDeductType(status), deductRates);
        return BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(rate));
    }

    private static DeductEnum toDeductType(AttendanceStatusEnum status) {
        switch (status) {
            case LATE: return DeductEnum.LATE_DEDUCT;
            case LEAVE_EARLY: return DeductEnum.LEAVE_EARLY_DEDUCT;
            case ABSENTEEISM: return DeductEnum.ABSENTEEISM_DEDUCT;
            case LEAVE: return DeductEnum.LEAVE_DEDUCT;
            default: throw new IllegalArgumentException("无对应扣款类型: " + status);
        }
    }

    private static int rateOf(DeductEnum type, Map<Integer, Integer> deductRates) {
        if (deductRates != null && deductRates.containsKey(type.getCode())) {
            return deductRates.get(type.getCode());
        }
        return type.getDefaultValue();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}