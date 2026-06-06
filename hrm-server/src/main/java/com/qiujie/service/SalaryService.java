package com.qiujie.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.entity.Salary;
import com.qiujie.entity.SalaryDeduct;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.DeductEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.SalaryMapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.mapper.StaffOvertimeMapper;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.vo.StaffSalaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-04-06
 */
@Service
public class SalaryService extends ServiceImpl<SalaryMapper, Salary> {


    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private SalaryDeductService salaryDeductService;


    @Autowired
    private AttendanceMapper attendanceMapper;


    @Autowired
    private StaffOvertimeMapper staffOvertimeMapper;

    public ResponseDTO add(Salary salary) {
        if (save(salary)) {
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


    public ResponseDTO edit(Salary salary) {
        if (updateById(salary)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        Salary salary = getById(id);
        if (salary != null) {
            return Response.success(salary);
        }
        return Response.error();
    }


    public ResponseDTO list(Integer current, Integer size, String name, Integer deptId, String month) {
        IPage<StaffSalaryVO> config = new Page<>(current, size);
        // 解决当搜索条件为空时，默认查询所有数据
        if (name == null) {
            name = "";
        }
        IPage<StaffSalaryVO> page;
        if (deptId == null) {
            page = this.salaryMapper.listStaffSalaryVO(config, name);
        } else {
            page = this.salaryMapper.listStaffDeptSalaryVO(config, name, deptId);
        }
        // 如果没有指明月份，就默认显示当前月份
        if (month == null) {
            Date datetime = new Date(System.currentTimeMillis());
            month = DateUtil.format(datetime, "yyyyMM");
        }
        List<StaffSalaryVO> staffSalaryVOList = page.getRecords();
        setSalaryInfo(month, staffSalaryVOList);
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", staffSalaryVOList);
        map.put("month", month);
        return Response.success(map);
    }

    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public void export(HttpServletResponse response, String month,String filename) throws IOException {
        List<StaffSalaryVO> list = this.salaryMapper.queryStaffSalaryVO();
        setSalaryInfo(month, list);
        EasyExcelUtil.write(response, list, filename, StaffSalaryVO.class);
    }

    /**
     * 设置工资的详细信息
     *
     * @param month
     * @param list
     */
    private void setSalaryInfo(String month, List<StaffSalaryVO> list) {
        for (StaffSalaryVO staffSalaryVO : list) {
            // 迟到扣款
            BigDecimal lateDeduct = BigDecimal.valueOf(this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.LATE.getCode(), month) * queryLateDeduct(staffSalaryVO));
            staffSalaryVO.setLateDeduct(lateDeduct);
            // 早退扣款
            BigDecimal leaveEarlyDeduct = BigDecimal.valueOf(this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.LEAVE_EARLY.getCode(), month) * queryLeaveEarlyDeduct(staffSalaryVO));
            staffSalaryVO.setLeaveEarlyDeduct(leaveEarlyDeduct);
            // 旷工扣款
            BigDecimal absenteeismDeduct = BigDecimal.valueOf(this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.ABSENTEEISM.getCode(), month) * queryAbsenteeismDeduct(staffSalaryVO));
            staffSalaryVO.setAbsenteeismDeduct(absenteeismDeduct);
            // 休假扣款
            List<Date> leaveDateList = this.attendanceMapper.queryLeaveDate(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.LEAVE.getCode(), month);
            int count = 0;
            for (Date date : leaveDateList) {
                // 不包括周末
                if (!DateUtil.isWeekend(date)) {
                    count++;
                }
            }
            BigDecimal leaveDeduct = (BigDecimal.valueOf(count * queryLeaveDeduct(staffSalaryVO)));
            staffSalaryVO.setLeaveDeduct(leaveDeduct);
            QueryWrapper<Salary> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("staff_id", staffSalaryVO.getStaffId()).eq("month", month);
            Salary one = getOne(queryWrapper);
            if (one != null) {
                BigDecimal monthOvertimeSalary = this.staffOvertimeMapper.sumMonthOvertimeSalary(staffSalaryVO.getStaffId(), month);
                // 如果员工当前月没有加班工资，加班工资为0
                monthOvertimeSalary = monthOvertimeSalary == null ? BigDecimal.valueOf(0) : monthOvertimeSalary;
                staffSalaryVO
                        .setBaseSalary(one.getBaseSalary())
                        .setOvertimeSalary(monthOvertimeSalary)
                        .setSubsidy(one.getSubsidy())
                        .setBonus(one.getBonus())
                        .setRemark(one.getRemark())
                        .setTotalSalary(one.getBaseSalary()
                                .add(one.getBonus())
                                .add(one.getSubsidy())
                                .add(monthOvertimeSalary)
                                .subtract(lateDeduct)
                                .subtract(leaveEarlyDeduct)
                                .subtract(absenteeismDeduct)
                                .subtract(leaveDeduct)
                                .subtract(staffSalaryVO.getSocialPay())
                                .subtract(staffSalaryVO.getHousePay()));
            }
        }
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
        List<Salary> list = EasyExcelUtil.read(inputStream, 1, Salary.class);
        // IService接口中的方法.批量插入数据
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO setSalary(Salary salary) {
        QueryWrapper<Salary> query = new QueryWrapper<>();
        // 设置日薪、时薪
        salary.setDaySalary(salary.getBaseSalary().divide(new BigDecimal("21.75"),3, RoundingMode.HALF_UP));
        salary.setHourSalary(salary.getBaseSalary().divide(new BigDecimal(174),3, RoundingMode.HALF_UP));
        query.eq("month", salary.getMonth()).eq("staff_id", salary.getStaffId());
        if (saveOrUpdate(salary, query)) {
            return Response.success();
        }
        return Response.error();
    }


    /**
     * 每次迟到扣款
     *
     * @param staffSalaryVO
     * @return
     */
    public Integer queryLateDeduct(StaffSalaryVO staffSalaryVO) {
        QueryWrapper<SalaryDeduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", staffSalaryVO.getDeptId()).eq("type_num", DeductEnum.LATE_DEDUCT);
        SalaryDeduct salaryDeduct = this.salaryDeductService.getOne(queryWrapper);
        return salaryDeduct != null ? salaryDeduct.getDeduct() : DeductEnum.LATE_DEDUCT.getDefaultValue();
    }

    /**
     * 每次早退扣款
     *
     * @param staffSalaryVO
     * @return
     */
    public Integer queryLeaveEarlyDeduct(StaffSalaryVO staffSalaryVO) {
        QueryWrapper<SalaryDeduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", staffSalaryVO.getDeptId()).eq("type_num", DeductEnum.LEAVE_EARLY_DEDUCT);
        SalaryDeduct salaryDeduct = this.salaryDeductService.getOne(queryWrapper);
        return salaryDeduct != null ? salaryDeduct.getDeduct() : DeductEnum.LEAVE_EARLY_DEDUCT.getDefaultValue();
    }

    /**
     * 每次旷工扣款
     *
     * @param staffSalaryVO
     * @return
     */
    public Integer queryAbsenteeismDeduct(StaffSalaryVO staffSalaryVO) {
        QueryWrapper<SalaryDeduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", staffSalaryVO.getDeptId()).eq("type_num", DeductEnum.ABSENTEEISM_DEDUCT);
        SalaryDeduct salaryDeduct = this.salaryDeductService.getOne(queryWrapper);
        return salaryDeduct != null ? salaryDeduct.getDeduct() : DeductEnum.ABSENTEEISM_DEDUCT.getDefaultValue();
    }

    /**
     * 每次休假扣款
     *
     * @param staffSalaryVO
     * @return
     */
    public Integer queryLeaveDeduct(StaffSalaryVO staffSalaryVO) {
        QueryWrapper<SalaryDeduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", staffSalaryVO.getDeptId()).eq("type_num", DeductEnum.LEAVE_DEDUCT);
        SalaryDeduct salaryDeduct = this.salaryDeductService.getOne(queryWrapper);
        return salaryDeduct != null ? salaryDeduct.getDeduct() : DeductEnum.LEAVE_DEDUCT.getDefaultValue();
    }
}




