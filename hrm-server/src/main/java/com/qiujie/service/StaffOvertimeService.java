package com.qiujie.service;


import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.config.HolidayConfig;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.*;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.OvertimeMapper;
import com.qiujie.mapper.SalaryMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.mapper.StaffOvertimeMapper;
import com.qiujie.util.DatetimeUtil;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.vo.OvertimeMonthVO;
import com.qiujie.vo.StaffOvertimeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 员工加班表 服务类
 * </p>
 *
 * @author qiujie
 * @since 2024-03-20
 */
@Service
public class StaffOvertimeService extends ServiceImpl<StaffOvertimeMapper, StaffOvertime> {

    @Autowired
    private StaffOvertimeMapper staffOvertimeMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private OvertimeMapper overtimeMapper;

    @Autowired
    private HolidayConfig holidayConfig;

    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private DatetimeUtil datetimeUtil;

    public ResponseDTO add(StaffOvertime staffOvertime) {
        if (save(staffOvertime)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO delete(Integer id) {
        if (removeById(id)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO edit(StaffOvertime staffOvertime) {
        if (updateById(staffOvertime)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        StaffOvertime staffOvertime = getById(id);
        if (staffOvertime != null) {
            return Response.success(staffOvertime);
        }
        return Response.error();
    }


    public ResponseDTO list(Integer current, Integer size, String name, Integer deptId, String month) {
        IPage<StaffOvertimeVO> config = new Page<>(current, size);
        // 解决当搜索条件为空时，默认查询所有数据
        if (name == null) {
            name = "";
        }
        IPage<StaffOvertimeVO> page;
        if (deptId == null) {
            page = this.staffMapper.listStaffOvertimeVO(config, name);
        } else {
            page = this.staffMapper.listStaffDeptOvertimeVO(config, name, deptId);
        }
        // 每页展示的数据
        List<StaffOvertimeVO> staffDeptVOList = page.getRecords();
        // 如果没有指明月份，就默认显示当前月份的加班数据
        if (month == null) {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }
        String[] monthDayList = this.datetimeUtil.getMonthDayList(month);
        for (StaffOvertimeVO staffDeptVO : staffDeptVOList) {
            // 获取当前月的日期，格式为yyyyMMdd
            List<HashMap<String, Object>> list = new ArrayList<>();
            for (String day : monthDayList) {
                HashMap<String, Object> map = new HashMap<>();
                StaffOvertime staffOvertime = this.staffOvertimeMapper.queryByStaffIdAndDate(staffDeptVO.getStaffId(), day);
                // 如果加班数据不存在，就重新设置数据
                if (staffOvertime == null) {
                    Date date = DateUtil.parse(day, "yyyyMMdd").toSqlDate();
                    map.put("message", OvertimeStatusEnum.NORMAL.getMessage());
                    map.put("tagType", OvertimeStatusEnum.NORMAL.getTagType());
                    map.put("overtimeDate", date);
                } else {
                    map.put("message", staffOvertime.getStatus().getMessage());
                    map.put("tagType", staffOvertime.getStatus().getTagType());
                    map.put("overtimeDate", staffOvertime.getOvertimeDate());
                }
                list.add(map);
            }
            staffDeptVO.setOvertimeList(list);
        }
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", staffDeptVOList);
        map.put("dayNum", monthDayList.length);
        map.put("month", month);
        return Response.success(map);
    }


    /**
     * 导出员工月加班报表
     *
     * @param response
     * @param month
     * @return
     */
    public void export(HttpServletResponse response, String month, String filename) throws IOException {
        List<OvertimeMonthVO> list = this.staffMapper.queryOvertimeMonthVO();
        for (OvertimeMonthVO overtimeMonthVO : list) {
            // 设置加班次数
            overtimeMonthVO.setOvertimeTimes(this.staffOvertimeMapper.countTimes(overtimeMonthVO.getStaffId(),
                    OvertimeStatusEnum.OVERTIME.getCode(), month));
            // 设置调休次数
            overtimeMonthVO.setTimeOffDays(this.staffOvertimeMapper.countTimes(overtimeMonthVO.getStaffId(),
                    OvertimeStatusEnum.TIME_OFF.getCode(), month));
        }
        EasyExcelUtil.write(response, list, filename, OvertimeMonthVO.class);
    }

    /**
     * 数据导入
     *
     * @param file
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO imp(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        List<StaffOvertime> list = EasyExcelUtil.read(inputStream, 1, StaffOvertime.class);
        for (StaffOvertime staffOvertime : list) {
            if (staffOvertime.getStaffId() == null
                    || staffOvertime.getOvertimeDate() == null
                    || staffOvertime.getMorStartTime() == null
                    || staffOvertime.getMorEndTime() == null
                    || staffOvertime.getAftStartTime() == null
                    || staffOvertime.getAftEndTime() == null) {
                continue;
            }
            Staff staff = this.staffMapper.selectById(staffOvertime.getStaffId());
            // 设置加班状态
            staffOvertime.setStatus(OvertimeStatusEnum.OVERTIME);
            // 设置加班时间
            BigDecimal totalOvertime = calculateTotalOvertime(staffOvertime);
            staffOvertime.setTotalOvertime(totalOvertime);
            // 以最近的一次工资为准
            Salary salary = this.salaryMapper.selectList(new QueryWrapper<Salary>().eq("staff_id", staff.getId()).orderByDesc("month")).get(0);
            // 如果是节假日
            if (this.datetimeUtil.isHoliday(staffOvertime.getOvertimeDate())) {
                // 设置加班类型
                staffOvertime.setTypeNum(OvertimeEnum.HOLIDAY_OVERTIME);
                Overtime overtime = this.overtimeMapper.selectOne(new QueryWrapper<Overtime>()
                        .eq("type_num", OvertimeEnum.HOLIDAY_OVERTIME)
                        .eq("dept_id", staff.getDeptId()));

                calculateOvertimeSalary(staffOvertime, totalOvertime, salary, overtime);
            } else if (DateUtil.isWeekend(staffOvertime.getOvertimeDate())) { // 休息日加班
                // 设置加班类型
                staffOvertime.setTypeNum(OvertimeEnum.DAY_OFF_OVERTIME);
                Overtime overtime = this.overtimeMapper.selectOne(new QueryWrapper<Overtime>()
                        .eq("type_num", OvertimeEnum.DAY_OFF_OVERTIME)
                        .eq("dept_id", staff.getDeptId()));
                // 如果不调休
                if (overtime.getTimeOffFlag() == 0) {
                    calculateOvertimeSalary(staffOvertime, totalOvertime, salary, overtime);
                } else {
                    // 当调休时，每天的加班时间不应该小于8小时，补休天数加一；否则没有补休
                    if (totalOvertime.compareTo(new BigDecimal(8)) != -1) {
                        staffOvertime.setStatus(OvertimeStatusEnum.TIME_OFF);
                    }
                }
            } else {
                // 设置加班类型
                staffOvertime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME); // 工作日加班
                Overtime overtime = this.overtimeMapper.selectOne(new QueryWrapper<Overtime>()
                        .eq("type_num", OvertimeEnum.WORKDAY_OVERTIME)
                        .eq("dept_id", staff.getDeptId()));
                calculateOvertimeSalary(staffOvertime, totalOvertime, salary, overtime);
            }
            QueryWrapper<StaffOvertime> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("staff_id", staff.getId()).eq("overtime_date",
                    staffOvertime.getOvertimeDate());
            if (!saveOrUpdate(staffOvertime, queryWrapper)) {
                return Response.error(BusinessStatusEnum.DATA_IMPORT_ERROR);
            }

        }
        return Response.success();
    }

    /**
     * 计算员工的加班工资
     *
     * @param staffOvertime
     * @param totalOvertime
     * @param salary
     * @param overtime
     */
    private void calculateOvertimeSalary(StaffOvertime staffOvertime, BigDecimal totalOvertime, Salary salary, Overtime overtime) {
        // 如果以小时计算加班费
        if (overtime.getCountType() == 0) {
            // 当以小时为单位计算加班费时，加班时间不应该小于2小时；否则没有加班费
            if (totalOvertime.compareTo(new BigDecimal(2)) != -1) {
                staffOvertime.setOvertimeSalary(salary.getHourSalary().multiply(overtime.getSalaryMultiple()).multiply(totalOvertime).add(overtime.getBonus()));
            } else {
                staffOvertime.setOvertimeSalary(new BigDecimal(0));
            }
        } else {
            // 当以日为单位计算加班费时，当天的加班时间不应该小于8小时；否则没有加班费
            if (totalOvertime.compareTo(new BigDecimal(8)) != -1) {
                staffOvertime.setOvertimeSalary(salary.getDaySalary().multiply(overtime.getSalaryMultiple()).add(overtime.getBonus()));
            } else {
                staffOvertime.setOvertimeSalary(new BigDecimal(0));
            }
        }
    }





    /**
     * 计算员工的加班时间
     *
     * @param staffOvertime
     * @return
     */
    private BigDecimal calculateTotalOvertime(StaffOvertime staffOvertime) {
        long morDiff = staffOvertime.getMorEndTime().getTime() - staffOvertime.getMorStartTime().getTime();
        long aftDiff = staffOvertime.getAftEndTime().getTime() - staffOvertime.getAftStartTime().getTime();
        return BigDecimal.valueOf((morDiff + aftDiff) / (1000 * 60 * 60.0));
    }

    public ResponseDTO queryByStaffIdAndDate(Integer id, String date) {
        StaffOvertime staffOvertime = this.staffOvertimeMapper.queryByStaffIdAndDate(id, date.replace("-", ""));
        if (staffOvertime == null) {
            staffOvertime = new StaffOvertime();
            staffOvertime.setStaffId(id).setOvertimeDate(DateUtil.parseDate(date).toSqlDate());
            if (this.datetimeUtil.isHoliday(DateUtil.parseDate(date).toSqlDate())) {
                staffOvertime.setTypeNum(OvertimeEnum.HOLIDAY_OVERTIME);
            } else if (DateUtil.isWeekend(DateUtil.parseDate(date))) {
                staffOvertime.setTypeNum(OvertimeEnum.DAY_OFF_OVERTIME);
            } else {
                staffOvertime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
            }
        }
        return Response.success(staffOvertime);
    }


    /**
     * 设置加班
     *
     * @param staffOvertime
     * @return
     */
    public ResponseDTO setOvertime(StaffOvertime staffOvertime) {
        QueryWrapper<StaffOvertime> queryWrapper = new QueryWrapper<>();
        Staff staff = this.staffMapper.selectById(staffOvertime.getStaffId());
        // 设置加班状态
        staffOvertime.setStatus(OvertimeStatusEnum.OVERTIME);
        // 以最近的一次工资为准
        Salary salary = this.salaryMapper.selectList(new QueryWrapper<Salary>().eq("staff_id", staff.getId()).orderByDesc("month")).get(0);
        Overtime overtime = this.overtimeMapper.selectOne(new QueryWrapper<Overtime>()
                .eq("type_num", staffOvertime.getTypeNum())
                .eq("dept_id", staff.getDeptId()));
        // 如果是休息日加班
        if (staffOvertime.getTypeNum() == OvertimeEnum.DAY_OFF_OVERTIME) {
            // 如果不调休
            if (overtime.getTimeOffFlag() == 0) {
                calculateOvertimeSalary(staffOvertime, staffOvertime.getTotalOvertime(), salary, overtime);
            } else {
                // 当调休时，每天的加班时间不应该小于8小时，才有补休；否则没有补休
                if (staffOvertime.getTotalOvertime().compareTo(new BigDecimal(8)) != -1) {
                    staffOvertime.setStatus(OvertimeStatusEnum.TIME_OFF);
                }
            }
        } else {
            calculateOvertimeSalary(staffOvertime, staffOvertime.getTotalOvertime(), salary, overtime);
        }
        queryWrapper.eq("staff_id", staff.getId()).eq("overtime_date",
                staffOvertime.getOvertimeDate());
        if (saveOrUpdate(staffOvertime, queryWrapper)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 查询员工的调休余额
     *
     * @param id 员工id
     * @return
     */
    public ResponseDTO queryTimeOffDaysByStaffId(Integer id) {
        Long days = this.staffOvertimeMapper.selectCount(new QueryWrapper<StaffOvertime>().eq("staff_id", id).eq("status", OvertimeStatusEnum.TIME_OFF));
        return Response.success(days);
    }
}




