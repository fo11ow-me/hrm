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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 加班计算深模块——收敛 {@code StaffOvertimeService} 中三处重复的加班规则。
 * <p>
 * 隐藏的复杂性：
 * <ul>
 *   <li><b>时长</b>：每天上午/下午四个时间点 → 小时数</li>
 *   <li><b>日期分类</b>：节假日 / 周末 / 工作日 → {@link OvertimeEnum}</li>
 *   <li><b>调休判定</b>：休息日 + 配置允许调休 + 时长 ≥ 8h → {@link OvertimeStatusEnum#TIME_OFF}</li>
 *   <li><b>计薪规则</b>：按小时（≥2h）或按天（≥8h），时薪/日薪 × 倍数 + 奖金</li>
 *   <li><b>引用数据兜底</b>：员工不存在、配置缺失、无薪资记录 → 不崩、计 0</li>
 * </ul>
 * 输出结果值对象 {@link OvertimeResult}，不含任何持久化。
 * </p>
 *
 * @see OvertimeResult
 */
@Component
public class OvertimeCalculator {

    private static final BigDecimal MIN_HOURLY_OVERTIME = new BigDecimal("2");
    private static final BigDecimal MIN_DAILY_OVERTIME = new BigDecimal("8");

    private final StaffMapper staffMapper;
    private final OvertimeMapper overtimeMapper;
    private final SalaryMapper salaryMapper;
    private final DatetimeUtil datetimeUtil;

    public OvertimeCalculator(StaffMapper staffMapper,
                              OvertimeMapper overtimeMapper,
                              SalaryMapper salaryMapper,
                              DatetimeUtil datetimeUtil) {
        this.staffMapper = staffMapper;
        this.overtimeMapper = overtimeMapper;
        this.salaryMapper = salaryMapper;
        this.datetimeUtil = datetimeUtil;
    }

    // ==================== 批量入口（引用数据由调用方预加载，避免 N+1） ====================

    /**
     * 批量场景入口——引用数据来自调用方维护的缓存。
     * <p>
     * 缓存 key 约定：{@code staffCache} 按 staffId；{@code configCache} 按 deptId_typeCode；
     * {@code salaryCache} 按 staffId（最近一条薪资）。缓存缺失时回退 DB 查询。
     *
     * @param staffId     员工 id
     * @param src         加班数据（工时时间点）
     * @param staffCache  员工缓存（可空）
     * @param configCache 加班配置缓存（可空）
     * @param salaryCache 薪资缓存（可空）
     * @return 计算结果，员工不存在时返回空结果
     */
    public OvertimeResult compute(int staffId, StaffOvertime src,
                                  Map<Integer, Staff> staffCache,
                                  Map<String, Overtime> configCache,
                                  Map<Integer, Salary> salaryCache) {
        Staff staff = src == null || src.getStaffId() == null
                ? null : loadStaff(src.getStaffId(), staffCache);
        if (staff == null) {
            return empty();
        }
        return computeFromReferences(src, staff,
                resolveConfig(src, staff, configCache),
                loadLatestSalary(src.getStaffId(), salaryCache),
                datetimeUtil::isHoliday);
    }

    // ==================== 单条入口（自动加载引用数据） ====================

    /**
     * 单条场景入口——内部自行加载引用数据。
     *
     * @param staffId 员工 id
     * @param src     加班数据
     * @return 计算结果
     */
    public OvertimeResult compute(int staffId, StaffOvertime src) {
        return compute(staffId, src, null, null, null);
    }

    // ==================== 纯计算（无 I/O，输入即输出） ====================

    /**
     * 纯函数——仅依赖传入的引用数据，无任何 I/O，可直接零 mock 单测。
     * <p>
     * 规则（与历史实现保持一致）：
     * <ol>
     *   <li>时长 = (morEnd-morStart) + (aftEnd-aftStart)，按小时</li>
     *   <li>类型：src 已带类型则信任之；否则按日期分类（节假日→HOLIDAY；周末→DAY_OFF；否则→WORKDAY），
     *       {@code isHoliday} 为 null 时仅按周末分类（无节假日配置的兜底）</li>
     *   <li>调休：DAY_OFF 且配置 timeOffFlag==1 且时长≥8h → TIME_OFF（不计薪）</li>
     *   <li>计薪：countType==0 且时长≥2h → 时薪×倍数×时长+奖金；countType==1 且时长≥8h → 日薪×倍数+奖金；否则 0</li>
     * </ol>
     *
     * @param isHoliday 节假日判定谓词（可空；null 时按周末分类）
     */
    public static OvertimeResult computeFromReferences(StaffOvertime src, Staff staff,
                                                       Overtime config, Salary latestSalary,
                                                       Predicate<Date> isHoliday) {
        if (src == null || src.getOvertimeDate() == null) {
            return empty();
        }
        BigDecimal total = totalOvertime(src);
        OvertimeEnum type = resolveType(src, isHoliday);
        if (config != null && isTimeOff(type, config, total)) {
            return new OvertimeResult(type, OvertimeStatusEnum.TIME_OFF, total, null);
        }
        BigDecimal salary = config != null && latestSalary != null
                ? calculateSalary(total, config, latestSalary) : null;
        OvertimeStatusEnum status = salary == null && config == null
                ? OvertimeStatusEnum.NORMAL
                : OvertimeStatusEnum.OVERTIME;
        return new OvertimeResult(type, status, total, salary);
    }

    // ==================== 私有 ====================

    private static boolean isTimeOff(OvertimeEnum type, Overtime config, BigDecimal total) {
        return type == OvertimeEnum.DAY_OFF_OVERTIME
                && config.getTimeOffFlag() != null && config.getTimeOffFlag() == 1
                && total.compareTo(MIN_DAILY_OVERTIME) >= 0;
    }

    private static BigDecimal calculateSalary(BigDecimal total, Overtime config, Salary salary) {
        if (config.getCountType() != null && config.getCountType() == 1) {
            // 按天：当日加班时长 ≥ 8h 才有
            if (total.compareTo(MIN_DAILY_OVERTIME) < 0) {
                return BigDecimal.ZERO;
            }
            return salary.getDaySalary().multiply(config.getSalaryMultiple()).add(config.getBonus());
        }
        // 按小时：当日加班时长 ≥ 2h 才有
        if (total.compareTo(MIN_HOURLY_OVERTIME) < 0) {
            return BigDecimal.ZERO;
        }
        return salary.getHourSalary().multiply(config.getSalaryMultiple()).multiply(total).add(config.getBonus());
    }

    private static BigDecimal totalOvertime(StaffOvertime src) {
        if (src.getMorStartTime() == null || src.getMorEndTime() == null
                || src.getAftStartTime() == null || src.getAftEndTime() == null) {
            return BigDecimal.ZERO;
        }
        long morDiff = src.getMorEndTime().getTime() - src.getMorStartTime().getTime();
        long aftDiff = src.getAftEndTime().getTime() - src.getAftStartTime().getTime();
        return BigDecimal.valueOf((morDiff + aftDiff) / (1000 * 60 * 60.0));
    }

    /** 类型：src 已明确类型则信任（单条设置场景手动指定）；否则按日期分类。 */
    private static OvertimeEnum resolveType(StaffOvertime src, Predicate<Date> isHoliday) {
        if (src.getTypeNum() != null) {
            return src.getTypeNum();
        }
        Date date = src.getOvertimeDate();
        if (isHoliday != null && isHoliday.test(date)) {
            return OvertimeEnum.HOLIDAY_OVERTIME;
        }
        if (cn.hutool.core.date.DateUtil.isWeekend(date)) {
            return OvertimeEnum.DAY_OFF_OVERTIME;
        }
        return OvertimeEnum.WORKDAY_OVERTIME;
    }

    private Staff loadStaff(int staffId, Map<Integer, Staff> staffCache) {
        if (staffCache != null && staffCache.containsKey(staffId)) {
            return staffCache.get(staffId);
        }
        return staffMapper.selectById(staffId);
    }

    private Overtime resolveConfig(StaffOvertime src, Staff staff, Map<String, Overtime> configCache) {
        OvertimeEnum type = resolveType(src, datetimeUtil::isHoliday);
        String key = staff.getDeptId() + "_" + type.getCode();
        if (configCache != null && configCache.containsKey(key)) {
            return configCache.get(key);
        }
        return overtimeMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Overtime>()
                .eq("type_num", type).eq("dept_id", staff.getDeptId()));
    }

    private Salary loadLatestSalary(int staffId, Map<Integer, Salary> salaryCache) {
        if (salaryCache != null && salaryCache.containsKey(staffId)) {
            return salaryCache.get(staffId);
        }
        return salaryMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Salary>()
                .eq("staff_id", staffId).orderByDesc("month")).stream().findFirst().orElse(null);
    }

    private static OvertimeResult empty() {
        return new OvertimeResult(null, OvertimeStatusEnum.NORMAL, BigDecimal.ZERO, null);
    }
}