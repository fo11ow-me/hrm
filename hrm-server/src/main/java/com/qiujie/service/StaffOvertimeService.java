package com.qiujie.service;


import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.config.HolidayConfig;
import com.qiujie.dto.OvertimeImportRow;import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.*;
import com.qiujie.enums.*;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.mapper.StaffOvertimeMapper;
import com.qiujie.overtime.OvertimeCalculator;
import com.qiujie.overtime.OvertimeResult;
import com.qiujie.util.AiHeaderMatcherImpl;
import com.qiujie.util.DatetimeUtil;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.util.SecurityUtil;
import com.qiujie.vo.OvertimeMonthVO;
import com.qiujie.vo.StaffOvertimeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import java.util.function.Consumer;

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
    private DatetimeUtil datetimeUtil;

    @Autowired
    private FileTaskCoordinator fileTaskCoordinator;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired(required = false)
    private AiHeaderMatcherImpl aiHeaderMatcher;

    @Autowired
    private OvertimeCalculator overtimeCalculator;

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
            OvertimeResult result = overtimeCalculator.compute(staffOvertime.getStaffId(), staffOvertime);
            staffOvertime.setTypeNum(result.typeNum())
                    .setStatus(result.status())
                    .setTotalOvertime(result.totalOvertime())
                    .setOvertimeSalary(result.overtimeSalary());
            QueryWrapper<StaffOvertime> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("staff_id", staffOvertime.getStaffId()).eq("overtime_date",
                    staffOvertime.getOvertimeDate());
            if (!update(staffOvertime, queryWrapper) || save(staffOvertime)) {
                return Response.error(BusinessStatusEnum.DATA_IMPORT_ERROR);
            }
        }
        return Response.success();
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
        OvertimeResult result = overtimeCalculator.compute(staffOvertime.getStaffId(), staffOvertime);
        staffOvertime.setTypeNum(result.typeNum())
                .setStatus(result.status())
                .setTotalOvertime(result.totalOvertime())
                .setOvertimeSalary(result.overtimeSalary());
        QueryWrapper<StaffOvertime> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("staff_id", staffOvertime.getStaffId()).eq("overtime_date",
                staffOvertime.getOvertimeDate());
        if (update(staffOvertime, queryWrapper) || save(staffOvertime)) {
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

    // ==================== 异步导入导出 ====================

    public ResponseDTO createImportTask(String uploadId) {
        String mergedKey = fileUploadService.completeUpload(uploadId);
        FileTaskCoordinator.TaskSubmission submission = fileTaskCoordinator.submitImport(
                new FileTaskCoordinator.ImportCommand(
                        TaskModuleEnum.STAFF_OVERTIME,
                        "overtime_import.xlsx",
                        mergedKey,
                        null,
                        securityUtil.getCurrentOperatorId()),
                new OvertimeImportHandler(),
                new FlexibleExcelImportReader<>(2, TaskModuleEnum.STAFF_OVERTIME,
                        OvertimeImportRow.class, aiHeaderMatcher));
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", submission.taskId());
        return Response.success("已提交", data);
    }

    public ResponseDTO createExportTask(String month, String filename) {
        Map<String, String> params = new HashMap<>();
        params.put("month", month);
        FileTaskCoordinator.TaskSubmission submission = fileTaskCoordinator.submitExport(
                new FileTaskCoordinator.ExportCommand(
                        TaskModuleEnum.STAFF_OVERTIME,
                        filename,
                        JSON.toJSONString(params),
                        securityUtil.getCurrentOperatorId()),
                new OvertimeExportHandler());
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", submission.taskId());
        return Response.success("已提交", data);
    }

    class OvertimeImportHandler implements ImportProcessor<OvertimeImportRow> {

        @Override
        public Class<OvertimeImportRow> getRowClass() {
            return OvertimeImportRow.class;
        }

        @Override
        public TaskModuleEnum getModule() {
            return TaskModuleEnum.STAFF_OVERTIME;
        }

        @Override
        public void processBatch(List<OvertimeImportRow> rows, Long taskId,
                                  Consumer<FileTaskError> errorCollector) {
            // 预加载参照数据
            Map<Integer, Staff> staffMap = new HashMap<>();
            Map<String, Overtime> overtimeConfigCache = new HashMap<>();
            Map<Integer, Salary> salaryCache = new HashMap<>();

            List<StaffOvertime> validList = new ArrayList<>();
            for (OvertimeImportRow row : rows) {
                if (row.getStaffId() == null || row.getOvertimeDate() == null
                        || row.getMorStartTime() == null || row.getMorEndTime() == null
                        || row.getAftStartTime() == null || row.getAftEndTime() == null) {
                    continue;
                }
                try {
                    Staff staff = staffMap.computeIfAbsent(row.getStaffId(),
                            id -> staffMapper.selectById(id));
                    if (staff == null) {
                        errorCollector.accept(new FileTaskError()
                                .setTaskId(taskId).setRowNum(row.getRowNum())
                                .setRawData(JSON.toJSONString(row)).setErrorMessage("员工不存在"));
                        continue;
                    }
                    StaffOvertime entity = toEntity(row);
                    // 加班类型由日期分类得出（无人工指定）
                    OvertimeResult result = overtimeCalculator.compute(row.getStaffId(), entity,
                            staffMap, overtimeConfigCache, salaryCache);
                    entity.setTypeNum(result.typeNum())
                            .setStatus(result.status())
                            .setTotalOvertime(result.totalOvertime())
                            .setOvertimeSalary(result.overtimeSalary());
                    validList.add(entity);
                } catch (Exception e) {
                    errorCollector.accept(new FileTaskError()
                            .setTaskId(taskId).setRowNum(row.getRowNum())
                            .setRawData(JSON.toJSONString(row)).setErrorMessage(e.getMessage()));
                }
            }
            if (!validList.isEmpty()) {
                for (StaffOvertime entity : validList) {
                    QueryWrapper<StaffOvertime> qw = new QueryWrapper<>();
                    qw.eq("staff_id", entity.getStaffId()).eq("overtime_date", entity.getOvertimeDate());
                    if (!update(entity, qw)) { save(entity); }
                }
            }
        }

        private StaffOvertime toEntity(OvertimeImportRow row) {
            return new StaffOvertime()
                    .setStaffId(row.getStaffId())
                    .setMorStartTime(row.getMorStartTime())
                    .setMorEndTime(row.getMorEndTime())
                    .setAftStartTime(row.getAftStartTime())
                    .setAftEndTime(row.getAftEndTime())
                    .setOvertimeDate(row.getOvertimeDate());
        }
    }

    class OvertimeExportHandler implements ExportProcessor<OvertimeMonthVO> {

        @Override
        public Class<OvertimeMonthVO> getRowClass() {
            return OvertimeMonthVO.class;
        }

        @Override
        public TaskModuleEnum getModule() {
            return TaskModuleEnum.STAFF_OVERTIME;
        }

        @Override
        public IPage<OvertimeMonthVO> queryPage(int current, int pageSize, String queryParamsJson) {
            Map<String, String> params = JSON.parseObject(queryParamsJson, Map.class);
            String month = params.get("month");

            IPage<OvertimeMonthVO> page = new Page<>(current, pageSize);
            List<OvertimeMonthVO> list = staffMapper.queryOvertimeMonthVO();
            for (OvertimeMonthVO vo : list) {
                vo.setOvertimeTimes(staffOvertimeMapper.countTimes(vo.getStaffId(),
                        OvertimeStatusEnum.OVERTIME.getCode(), month));
                vo.setTimeOffDays(staffOvertimeMapper.countTimes(vo.getStaffId(),
                        OvertimeStatusEnum.TIME_OFF.getCode(), month));
            }
            int from = (current - 1) * pageSize;
            int to = Math.min(from + pageSize, list.size());
            if (from >= list.size()) {
                page.setRecords(Collections.emptyList());
            } else {
                page.setRecords(list.subList(from, to));
            }
            page.setTotal(list.size());
            return page;
        }
    }

}






