package com.qiujie.service;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.entity.FileTask;
import com.qiujie.entity.Salary;
import com.qiujie.entity.SalaryDeduct;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.DeductEnum;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.enums.TaskTypeEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.SalaryMapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.mapper.StaffOvertimeMapper;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.util.SecurityUtil;
import com.qiujie.vo.StaffSalaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
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

    @Autowired
    private FileTaskService fileTaskService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private com.qiujie.storage.MinioStorageService storageService;

    @Autowired
    @Qualifier("fileTaskExecutor")
    private ThreadPoolTaskExecutor fileTaskExecutor;

    @Autowired
    private SecurityUtil securityUtil;

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
        DateTime dt = DateUtil.parse(month, "yyyyMM");
        Date startDate = dt.toSqlDate();
        Date endDate = DateUtil.offsetMonth(dt, 1).toSqlDate();
        for (StaffSalaryVO staffSalaryVO : list) {
            // 迟到扣款
            BigDecimal lateDeduct = BigDecimal.valueOf(this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.LATE.getCode(), startDate, endDate) * queryLateDeduct(staffSalaryVO));
            staffSalaryVO.setLateDeduct(lateDeduct);
            // 早退扣款
            BigDecimal leaveEarlyDeduct = BigDecimal.valueOf(this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.LEAVE_EARLY.getCode(), startDate, endDate) * queryLeaveEarlyDeduct(staffSalaryVO));
            staffSalaryVO.setLeaveEarlyDeduct(leaveEarlyDeduct);
            // 旷工扣款
            BigDecimal absenteeismDeduct = BigDecimal.valueOf(this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.ABSENTEEISM.getCode(), startDate, endDate) * queryAbsenteeismDeduct(staffSalaryVO));
            staffSalaryVO.setAbsenteeismDeduct(absenteeismDeduct);
            // 休假扣款
            List<Date> leaveDateList = this.attendanceMapper.queryLeaveDate(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.LEAVE.getCode(), startDate, endDate);
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
        if (update(salary, query) || save(salary)) {
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

    /**
     * 通过三阶段上传完成后创建异步导入任务。
     */
    public ResponseDTO createImportTask(String uploadId) {
        String mergedKey = fileUploadService.completeUpload(uploadId);
        FileTask task = fileTaskService.createTask(TaskTypeEnum.IMPORT, TaskModuleEnum.SALARY,
                "salary_import.xlsx", mergedKey, null, getCurrentOperatorId());
        fileTaskExecutor.execute(() -> runImportTask(task.getId()));
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        return Response.success(result);
    }

    /**
     * 创建异步导出任务（支持大文件）
     */
    public ResponseDTO createExportTask(String month, String filename) {
        FileTask task = fileTaskService.createTask(TaskTypeEnum.EXPORT, TaskModuleEnum.SALARY,
                filename, null, month, getCurrentOperatorId());
        fileTaskExecutor.execute(() -> runExportTask(task.getId(), month, filename));
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        return Response.success(result);
    }

    private void runImportTask(Long taskId) {
        try {
            fileTaskService.markRunning(taskId);
            FileTask task = fileTaskService.getById(taskId);
            File sourceFile = resolveImportFile(task.getSourceFilePath());
            List<Salary> list = EasyExcelUtil.read(new java.io.FileInputStream(sourceFile), 1, Salary.class);
            fileTaskService.setTotalCount(taskId, list.size());
            if (saveBatch(list)) {
                fileTaskService.deleteSourceFile(taskId);
                fileTaskService.finish(taskId, TaskStatusEnum.SUCCESS);
            } else {
                fileTaskService.finish(taskId, TaskStatusEnum.PARTIAL_SUCCESS);
            }
        } catch (Exception e) {
            fileTaskService.fail(taskId, e);
        }
    }

    private File resolveImportFile(String path) {
        if (path == null || path.contains(File.separator) || path.startsWith("/")) {
            return new File(path);
        }
        File tempFile = new File(fileTaskService.buildTaskFile("task-source", "import.xlsx").getParentFile(), path.replace('/', '_'));
        tempFile.getParentFile().mkdirs();
        try (java.io.InputStream in = storageService.get(path)) {
            cn.hutool.core.io.FileUtil.writeFromStream(in, tempFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download from MinIO: " + path, e);
        }
        return tempFile;
    }

    private void runExportTask(Long taskId, String month, String filename) {
        try {
            fileTaskService.markRunning(taskId);
            List<StaffSalaryVO> list = salaryMapper.queryStaffSalaryVO();
            setSalaryInfo(month, list);
            String exportName = filename != null ? filename : month + "薪资报表";
            File file = fileTaskService.buildTaskFile("task-result", exportName + ".xlsx");
            com.alibaba.excel.EasyExcel.write(file, StaffSalaryVO.class).sheet(exportName).doWrite(list);
            fileTaskService.setTotalCount(taskId, list.size());
            fileTaskService.setResultFile(taskId, fileTaskService.uploadToMinio(file, "task-result"));
            fileTaskService.finish(taskId, TaskStatusEnum.SUCCESS);
        } catch (Exception e) {
            fileTaskService.fail(taskId, e);
        }
    }

    private Integer getCurrentOperatorId() {
        try {
            return securityUtil != null ? securityUtil.getCurrentOperatorId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}




