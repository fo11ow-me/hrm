package com.qiujie;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.*;
import com.qiujie.enums.*;
import com.qiujie.mapper.*;
import com.qiujie.service.*;
import com.qiujie.util.DatetimeUtil;
import com.qiujie.util.EnumUtil;
import com.qiujie.util.HutoolExcelUtil;
import com.qiujie.util.RedisUtil;
import com.qiujie.vo.StaffAttendanceVO;
import com.qiujie.vo.StaffLeaveVO;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HrmApplicationTests {

    @Autowired
    private StaffService staffService;

    @Resource
    private StaffMapper staffMapper;

    @Resource
    private RoleMenuService roleMenuService;

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Resource
    private LoginService loginService;

    @Resource
    private MenuService menuService;

    @Resource
    private InsuranceMapper insuranceMapper;

    @Resource
    private DeptMapper deptMapper;

    @Resource
    private AttendanceMapper attendanceMapper;


    @Resource
    private AttendanceService attendanceService;

    @Resource
    private SalaryService salaryService;

    @Resource
    private HomeService homeService;

    @Resource
    private DocsMapper docsMapper;


    @Resource
    private OvertimeService overtimeService;

    @Test
    void test1() {
        ResponseDTO resultDTO = new ResponseDTO();
        System.out.println(resultDTO);
    }

    @Test
    void test2() {
        ResponseDTO resultDTO;
        resultDTO = new ResponseDTO(200, "success", "ok");
        System.out.println(resultDTO);
    }

    /**
     * 测试项目路径
     */
    @Test
    void test3() {
        String path = System.getProperty("user.dir");
        System.out.println(path + "/src/main/java");
    }

    @Test
    void test4() {
        List<Character> list = new ArrayList<>();
        list.add('a');
        list.add('b');
        list.add('c');
        System.out.println(list.indexOf('a'));
        System.out.println(list.indexOf('b'));
        System.out.println(list.indexOf('c'));
        System.out.println(list.remove(1)); // list.remove(int index)返回当前删除的对象
    }

    @Test
    void test5() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        for (Integer integer : list) {
            System.out.println(integer);
        }
        System.out.println(list.remove(1));
        for (Integer integer : list) {
            System.out.println(integer);
        }
    }

    /**
     * 测试添加
     */
    @Test
    void test6() {
        Staff staff = new Staff();
        staff.setCode("staff_6");
        staff.setName("harden");
        staff.setPassword("010101");
        System.out.println("结果为：" + this.staffService.add(staff).getMessage());
    }

    /**
     * 测试更新
     */
    @Test
    void test7() {
        Staff staff = new Staff();
        // staff.setId(6);
        staff.setPassword("0000");
        staff.setRemark("硕士");
        System.out.println("结果为：" + this.staffService.updateById(staff));
    }

    /**
     * 测试逻辑删除
     */
    @Test
    void test8() {
        System.out.println("结果为：" + this.staffService.delete(5));
    }

    /**
     * 分页查询
     */
    @Test
    void test9() {
        IPage<StaffLeaveVO> config = new Page<>(1, 10);
        List<Integer> ids = new ArrayList<>();
//        ids.add(1);
//        ids.add(3);
//        ids.add(10);
//        ids.add(42);
//        ids.add(43);
        IPage<StaffLeaveVO> page = this.staffLeaveMapper.listStaffLeaveVO(config, "",null, ids);
        System.out.println("页数："+page.getPages());
        System.out.println("数据："+page.getRecords().size());
        System.out.println("总数："+page.getTotal());
        System.out.println("数目："+page.getSize());
        System.out.println("当前："+page.getCurrent());
    }

    @Test
    void test13() {
        Staff staff = new Staff();
        staff.setId(12);
        // System.out.println(this.staffService.getById(12));
        System.out.println(this.staffMapper.selectById(12));
    }

    @Test
    void test14() {
        QueryWrapper<RoleMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", 1);
        System.out.println("结果为！" + this.roleMenuMapper.delete(wrapper));
        if (this.roleMenuService.remove(wrapper)) {
            System.out.println("删除成功！");
        } else {
            System.out.println("删除失败！");
        }
    }

    @Test
    void test15() {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(2);
        roleMenu.setMenuId(3);
//        roleMenu.setStatus(1);
        QueryWrapper<RoleMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", 2).eq("menu_id", 3);
        this.roleMenuService.saveOrUpdate(roleMenu, wrapper);
    }


    @Test
    void test19() {
        System.out.println("错误为：" + BusinessStatusEnum.SUCCESS);
    }

    @Test
    void test22() {
        Staff staff = this.staffService.getById(8);
    }

    @Test
    void test23() {
        List<Map<String, Map<String, Object>>> enumItemList = EnumUtil.getEnumItemList(AuditStatusEnum.class);
        for (Map<String, Map<String, Object>> stringMapMap : enumItemList) {
            System.out.println(stringMapMap);
        }
    }

    @Test
    void test24() {

    }

    @Test
    void test25() {

    }

    @Test
    void test26() throws FileNotFoundException {
        File file = new File("C:\\Users\\asus\\Desktop\\员工考勤表.xlsx");
        InputStream inputStream = new FileInputStream(file);
        List<Attendance> list = HutoolExcelUtil.readExcel(inputStream, 1, Attendance.class);
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm");
        for (Attendance item : list) {
            System.out.println(dateFormat.format(item.getAttendanceDate()));
        }
    }

    @Test
    void test27() {
//        for (String s : DatetimeUtil.getMonthDayList("202203")) {
//            System.out.println(s);
//        }
    }

    @Test
    void test28() {
        System.out.println(DateUtil.parse("20220301", "yyyyMMdd"));
    }

    @Test
    void test29() {
        QueryWrapper<Salary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("staff_id", 1).eq("month", "202304");
        Salary one = this.salaryService.getOne(queryWrapper);
        if (one == null) {
            System.out.println("没有");
        } else {
            System.out.println("有啊");
        }
    }

    @Test
    void test30() {
        System.out.println(new Date());
    }

    @Test
    void test31() {
        System.out.println(DateUtil.thisYear());
    }

    @Test
    void test32() {
        System.out.println(DateUtil.thisMonth());
    }

    @Test
    void test33() {
        System.out.println(DateUtil.beginOfYear(new Date(System.currentTimeMillis())));
    }

    @Test
    void test34() {
        DateTime startDate = DateUtil.beginOfMonth(new Date());
        int length = DateUtil.lengthOfMonth(DateUtil.thisMonth() + 1, DateUtil.isLeapYear(DateUtil.thisYear()));
        System.out.println(DateUtil.thisMonth());
        // DateUtil.thisYear();
    }

    @Test
    void test35() {
        DateTime start = DateUtil.beginOfMonth(new Date());
        DateUtil.offsetDay(start, 1);
        DateTime end = DateUtil.endOfMonth(new Date());
        // System.out.println(start + "--------" + end);
        System.out.println(DateUtil.format(new Date(), "yyyyMM"));
    }

    @Test
    void test36() {
        ResponseDTO departmentData = this.homeService.queryDepartment();
        System.out.println(departmentData.getData());
    }

    @Test
    void test37() {

        List<Map<String, Object>> enumList = EnumUtil.getEnumList(LeaveEnum.class);
        for (Map<String, Object> map : enumList) {
            for (AttendanceStatusEnum attendanceStatusEnum : AttendanceStatusEnum.values()) {
                if (map.get("code") == attendanceStatusEnum.getCode()) {
                    map.put("tagType", attendanceStatusEnum.getTagType());
                }
            }
        }
    }

    @Autowired
    private DatetimeUtil datetimeUtil;

    @Test
    void test38() {
        Integer id = 1;
        String month = null;
        if (month == null || month == "") {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }
        String[] monthDayList = this.datetimeUtil.getMonthDayList(month);
        // 考勤状态表
        List<HashMap<String, Object>> list = new ArrayList<>();
        for (String day : monthDayList) {
            HashMap<String, Object> map = new HashMap<>();
            Attendance attendance = this.attendanceMapper.queryByStaffIdAndDate(id, day);
            if (attendance == null) {
                java.sql.Date date = DateUtil.parse(day, "yyyyMMdd").toSqlDate();
                if (DateUtil.isWeekend(date)) {
                    map.put("message", AttendanceStatusEnum.LEAVE.getMessage());
                    map.put("tagType", AttendanceStatusEnum.LEAVE.getTagType());
                } else {
                    map.put("message", AttendanceStatusEnum.NORMAL.getMessage());
                    map.put("tagType", AttendanceStatusEnum.NORMAL.getTagType());
                }
            } else {
                map.put("message", attendance.getStatus().getMessage());
                map.put("tagType", attendance.getStatus().getTagType());

            }
            list.add(map);
        }
        for (HashMap<String, Object> stringObjectHashMap : list) {
            System.out.println(stringObjectHashMap);
        }
    }

    @Test
    void test39() {
        Integer current = 1;
        Integer size = 10;
        String name = null;
        String month = null;
        IPage<StaffAttendanceVO> config = new Page<>(current, size);
        // 解决当搜索条件为空时，默认查询所有数据
        if (name == null) {
            name = "";
        }
        // 如果没有指明月份，就默认显示当前月份的考勤数据
        if (month == null) {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }
        IPage<StaffAttendanceVO> page = this.staffMapper.listStaffAttendanceVO(config, name);
        // 每页展示的数据
        List<StaffAttendanceVO> staffDeptVOList = page.getRecords();
        String[] monthDayList = this.datetimeUtil.getMonthDayList(month);
        for (StaffAttendanceVO staffDeptVO : staffDeptVOList) {
            // 获取当前月的日期，格式为yyyyMMdd
            List<HashMap<String, Object>> list = new ArrayList<>();
            for (String day : monthDayList) {
                HashMap<String, Object> map = new HashMap<>();
                Attendance attendance = this.attendanceMapper.queryByStaffIdAndDate(staffDeptVO.getStaffId(), day);
                // 如果考勤数据不存在，就重新设置数据
                if (attendance == null) {
                    java.sql.Date date = DateUtil.parse(day, "yyyyMMdd").toSqlDate();
                    // 如果是周末就休假
                    if (DateUtil.isWeekend(date)) {
                        map.put("message", AttendanceStatusEnum.LEAVE.getMessage());
                        map.put("tagType", AttendanceStatusEnum.LEAVE.getTagType());
                    } else {
                        map.put("message", AttendanceStatusEnum.NORMAL.getMessage());
                        map.put("tagType", AttendanceStatusEnum.NORMAL.getTagType());
                    }
                } else {
                    map.put("message", attendance.getStatus().getMessage());
                    map.put("tagType", attendance.getStatus().getTagType());
                }
                list.add(map);
            }
            staffDeptVO.setAttendanceList(list);
        }
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", staffDeptVOList);
        map.put("dayNum", monthDayList.length);
        map.put("month", month);
        System.out.println(map);
    }


    @Test
    void test40() {
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("staff_id", 1).eq("attendance_date", new Date("2023-01-01"));
        Attendance attendance = this.attendanceService.getOne(queryWrapper);
        System.out.println(attendance);

    }

    @Test
    void test41() {
//        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("id",200);
        Staff staff = this.staffService.getById(2000);
        System.out.println(staff);

    }

    @Resource
    private SalaryMapper salaryMapper;

    @Test
    void test44() {
        List<Salary> salaries = this.salaryMapper.selectList(new QueryWrapper<Salary>().eq("staff_id", 1).orderByDesc("month"));
        System.out.println(salaries);

    }


    @Test
    void test45() {
//        String d = "2024-03-23";
//        System.out.println(DateUtil.isWeekend(DateUtil.parseDate(d)));
        System.out.println(DateUtil.format(new Date(), "yyyy-MM-dd"));
    }


    @Test
    void test46() {
//        int res = this.staffMapper.delete(new QueryWrapper<Staff>().eq("status", 0).orderByDesc("create_time").last("limit 1"));
//        System.out.println(res);
        List<Staff> staffList = this.staffMapper.selectList(new QueryWrapper<Staff>().eq("status", 0).orderByAsc("create_time").last("limit 1"));
        System.out.println(staffList);
    }

    @Resource
    private PasswordEncoder passwordEncoder;

    @Test
    void test47() {
        System.out.println(passwordEncoder.encode("123").length());
        System.out.println(passwordEncoder.encode("123").length());
        System.out.println(passwordEncoder.matches("123", "$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm"));
    }

    @Test
    void test48() {
        List<Staff> list = this.staffMapper.selectList(new QueryWrapper<Staff>().eq("gender", GenderEnum.FEMALE));
        System.out.println(list);
        ;
    }

    @Resource
    private RedisUtil redisUtil;

    @Test
    void test49() {
        redisUtil.set("name", "qiu");
//        System.out.println(redisUtil.get("name"));
    }

    @Test
    void test50() {
        Staff staff = new Staff().setName("qiujie").setRemark("厉害").setAddress("上海").setCreateTime(new Timestamp(System.currentTimeMillis()));
        redisUtil.set("staff", staff.toString());
        System.out.println(redisUtil.get("staff"));
    }

    @Test
    void test51() {
        Staff staff = this.staffService.getById(1);
        System.out.println(staff);
//        System.out.println(redisUtil.get("name"));
    }


    @Autowired
    private ProcessEngine processEngine;

    @Test
    void test52() {
        TaskService taskService = processEngine.getTaskService();
        List<Task> list = taskService.createTaskQuery().list();
        for (Task task : list) {
            System.out.println("任务为：" + task);
        }
    }


    /**
     * 流程部署
     */
    @Test
    void test53() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("processes/leave.bpmn20.xml")
                .name("请假流程部署")
                .deploy();
        System.out.println("流程部署id" + deployment.getId());
        System.out.println("流程部署key" + deployment.getKey());
        System.out.println("流程部署名称" + deployment.getName());
    }


    /**
     * 启动一个流程实例
     */
    @Test
    void test54() {
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = defaultProcessEngine.getRuntimeService();
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("leave");
        System.out.println("id:" + instance.getId());
        System.out.println("定义id:" + instance.getProcessDefinitionId());
        System.out.println("实例id:" + instance.getProcessInstanceId());
        System.out.println("部署id:" + instance.getDeploymentId());
        System.out.println("key:" + instance.getProcessDefinitionKey());
        System.out.println("name:" + instance.getName());
    }


    /**
     * 查询员工的待处理任务
     * 挂起之后，仍能查询到员工的待处理任务，只是无法处理
     */
    @Test
    void test55() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        List<org.activiti.engine.task.Task> list = taskService.createTaskQuery().processDefinitionKey("leave").processInstanceBusinessKey("19").list();
        for (Task task : list) {
            System.out.println("任务id:" + task.getId());
            System.out.println("流程实例id:" + task.getProcessInstanceId());
            System.out.println("流程定义id:" + task.getProcessDefinitionId());
            System.out.println("任务定义key:" + task.getTaskDefinitionKey());
            System.out.println("任务处理人:" + task.getAssignee());
            System.out.println("任务名称:" + task.getName());
        }

    }

    /**
     * 完成任务
     */
    @Test
    void test56() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
//        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").taskAssignee("staff_2").list();
        List<org.activiti.engine.task.Task> list = taskService.createTaskQuery().processDefinitionKey("leave").processInstanceBusinessKey("19").list();
        List<Staff> staffList = this.staffMapper.queryByRole("hr");
        String hr = StringUtils.join(staffList.stream().map(Staff::getId).toList(), ",");
        Map<String, Object> map = new HashMap<>();
        map.put("hr", hr);
        for (Task task : list) {
            taskService.complete(task.getId(), map);
        }

    }


    /**
     * 查询流程定义
     */
    @Test
    void test57() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("leave")
                .orderByProcessDefinitionVersion()
                .desc()
                .list();
        for (ProcessDefinition instance : processDefinitionList) {
            System.out.println("id:" + instance.getId());
            System.out.println("版本:" + instance.getVersion());
            System.out.println("部署id:" + instance.getDeploymentId());
            System.out.println("key:" + instance.getKey());
            System.out.println("name:" + instance.getName());
        }
    }


    /**
     * 删除流程部署以及流程定义信息
     */
    @Test
    void test58() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("leave")
                .orderByProcessDefinitionVersion()
                .desc()
                .list();
        for (ProcessDefinition instance : processDefinitionList) {
            repositoryService.deleteDeployment(instance.getDeploymentId(), true);
        }
    }


    /**
     * 查询历史信息
     */
    @Test
    void test59() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        HistoryService historyService = processEngine.getHistoryService();
        HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery();
        List<HistoricActivityInstance> list = historicActivityInstanceQuery.processInstanceId("bceceb85-00a2-11ef-8e10-e02e0bf8bf06").list();
        for (HistoricActivityInstance historicActivityInstance : list) {
            System.out.println(historicActivityInstance.getActivityName());
        }
    }


    @Autowired
    private StaffLeaveService staffLeaveService;

    /**
     * 将activiti的数据库表与实际业务表进行关联，添加BusinessKey
     */
    @Test
    void test60() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        StaffLeave staffLeave = this.staffLeaveService.getOne(new QueryWrapper<StaffLeave>().eq("id", 19));
        Map<String, Object> map = new HashMap<>();
        map.put("staff", staffLeave.getStaffId());
        // 启动流程实例的过程中，添加BusinessKey
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("leave", String.valueOf(19), map);
        System.out.println(instance.getBusinessKey());
    }


    /**
     * 将流程定义下的全部的流程实例进行挂起或激活
     */
    @Test
    void test61() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        // 查询流程定义的信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("leave").singleResult();
        // 获取当前流程定义的实例是否都是被挂起的
        if (processDefinition.isSuspended()) {
            // 如果是挂起，则激活全部流程实例
//            repositoryService.activateProcessDefinitionById(processDefinition.getId()); // 仅仅是激活了流程定义
            repositoryService.activateProcessDefinitionById(processDefinition.getId(), true, null);
            System.out.println("流程定义id:" + processDefinition.getId() + "已激活");
        } else {
            // 如果正常，则挂起全部流程实例
//            repositoryService.suspendProcessDefinitionById(processDefinition.getId()); // 仅仅是挂起了流程定义
            repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);
            System.out.println("流程定义id:" + processDefinition.getId() + "已挂起");
        }
    }


    /**
     * 挂起或激活单个流程实例
     */
    @Test
    void test62() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processDefinitionKey("leave").processInstanceBusinessKey("19").singleResult();
        if (instance.isSuspended()) {
            // 如果是挂起，则激活
            runtimeService.activateProcessInstanceById(instance.getId());
            System.out.println("流程实例id:" + instance.getId() + "已激活");
        } else {
            // 如果正常，则挂起
            runtimeService.suspendProcessInstanceById(instance.getId());
            System.out.println("流程实列id:" + instance.getId() + "已挂起");
        }
    }


    /**
     * 启动一个流程实例，并设置对应的负责人
     */
    @Test
    void test63() {
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = defaultProcessEngine.getRuntimeService();
        Map<String, Object> map = new HashMap<>();
        map.put("staff", 1);
        map.put("hr", 2);
        map.put("manager", 3);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("leave", map);
        System.out.println("id:" + instance.getId());
        System.out.println("定义id:" + instance.getProcessDefinitionId());
        System.out.println("实例id:" + instance.getProcessInstanceId());
        System.out.println("部署id:" + instance.getDeploymentId());
        System.out.println("key:" + instance.getProcessDefinitionKey());
        System.out.println("name:" + instance.getName());
    }

    @Test
    void test64() {
        List<Staff> staffList = this.staffMapper.queryByRole("hr");
        String string = staffList.stream().map(Staff::getId).toList().toString();
        System.out.println(string);
    }


    /**
     * 将activiti的数据库表与实际业务表进行关联，添加BusinessKey
     */
    @Test
    void test65() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> map = new HashMap<>();
        map.put("staff", "admin");
        // 启动流程实例的过程中，添加BusinessKey
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("leave", String.valueOf(19), map);
        System.out.println(instance.getBusinessKey());
    }


    /**
     * 查询员工的待处理任务
     * 挂起之后，仍能查询到员工的待处理任务，只是无法处理
     */
    @Test
    void test66() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").taskAssignee("admin").list();
        for (Task task : list) {
            if (task != null) {
                System.out.println("任务id:" + task.getId());
                System.out.println("流程实例id:" + task.getProcessInstanceId());
                System.out.println("流程定义id:" + task.getProcessDefinitionId());
                System.out.println("任务定义key:" + task.getTaskDefinitionKey());
                System.out.println("任务处理人:" + task.getAssignee());
                System.out.println("任务名称:" + task.getName());
            }
        }

    }


    /**
     * 完成任务
     */
    @Test
    void test67() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
//        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").taskAssignee("staff_2").list();
        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").processInstanceBusinessKey("19").list();
        List<Staff> staffList = this.staffMapper.queryByRole("hr");
        Map<String, Object> map = new HashMap<>();
        map.put("hr", staffList.stream().map(Staff::getCode).collect(Collectors.joining(",")));
        for (Task task : list) {
            if (task != null) {
                taskService.complete(task.getId(), map);
            }
        }

    }



    @Autowired
    private RuntimeService runtimeService;


    /**
     * 查询员工的组任务
     * 挂起之后，仍能查询到员工的待处理任务，只是无法处理
     */
    @Test
    void test68() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").taskCandidateOrAssigned("admin").list();
        for (Task task : list) {
            if (task != null) {
                System.out.println("任务id:" + task.getId());
                System.out.println("流程实例id:" + task.getProcessInstanceId());
                System.out.println("流程定义id:" + task.getProcessDefinitionId());
                System.out.println("任务定义key:" + task.getTaskDefinitionKey());
                System.out.println("任务处理人:" + task.getAssignee());
                System.out.println("任务名称:" + task.getName());
                System.out.println("拥有者:" + task.getOwner());
                ProcessInstance instance = this.runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                System.out.println("任务BusinessKey:"+instance.getBusinessKey());
            }
        }
    }


    /**
     * 拾取任务
     */
    @Test
    void test69() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").processInstanceBusinessKey("19").list();
        for (Task task : list) {
            if (task != null) {
                System.out.println("任务id:" + task.getId());
                System.out.println("流程实例id:" + task.getProcessInstanceId());
                System.out.println("流程定义id:" + task.getProcessDefinitionId());
                System.out.println("任务定义key:" + task.getTaskDefinitionKey());
                System.out.println("任务处理人:" + task.getAssignee());
                System.out.println("任务名称:" + task.getName());
                taskService.claim(task.getId(), "staff_2");
            }
        }
    }


    /**
     * 完成人事审批组任务
     */
    @Test
    void test70() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
//        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").taskAssignee("staff_2").list();
        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").processInstanceBusinessKey("19").list();
        List<Staff> staffList = this.staffMapper.queryByRole("manager");
        Map<String, Object> variables = new HashMap<>();
        variables.put("manager", staffList.stream().map(Staff::getCode).collect(Collectors.joining(",")));
        Map<String, Object> transientVariables = new HashMap<>();
        transientVariables.put("status", 1);
        for (Task task : list) {
            if (task != null) {
                taskService.complete(task.getId(), variables, transientVariables);
            }
        }

    }

    /**
     * 完成经理审批组任务
     */
    @Test
    void test71() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
//        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").taskAssignee("staff_2").list();
        List<Task> list = taskService.createTaskQuery().processDefinitionKey("leave").processInstanceBusinessKey("19").list();
        Map<String, Object> transientVariables = new HashMap<>();
        transientVariables.put("status", 1);
        for (Task task : list) {
            if (task != null) {
                taskService.complete(task.getId(), null, transientVariables);
            }
        }

    }

    @Test
    void test72() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processDefinitionKey("leave").processInstanceBusinessKey(String.valueOf(19)).singleResult();
        System.out.println("id:" + instance.getId());
        System.out.println("定义id:" + instance.getProcessDefinitionId());
        System.out.println("实例id:" + instance.getProcessInstanceId());
        System.out.println("部署id:" + instance.getDeploymentId());
        System.out.println("key:" + instance.getProcessDefinitionKey());
        System.out.println("name:" + instance.getName());
//        runtimeService.deleteProcessInstance(instance.getId(),"申请撤销");
    }

    @Autowired
    private StaffLeaveMapper staffLeaveMapper;

    @Test
    void test73() {
        List<StaffLeave> staffLeaveList = this.staffLeaveMapper.selectList(new QueryWrapper<StaffLeave>().eq("staff_id", 1)
                .and(i -> i
                        .eq("status", AuditStatusEnum.UNAUDITED).or()
                        .eq("status", AuditStatusEnum.REJECT).or()
                        .eq("status", AuditStatusEnum.AUDITING))
        );
        System.out.println(staffLeaveList);
    }

    @Test
    void test74() {
        UpdateWrapper<Staff> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("remark","四川南充的").eq("id",1);
        boolean update = this.staffService.update(updateWrapper);
//        List<Staff> staff = this.staffMapper.selectList(new QueryWrapper<Staff>().eq("id", 1));
        System.out.println("结果为："+update);

    }

    @Autowired
    private RepositoryService repositoryService;

    @Test
    void test75() {
// 部署流程定义
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("path/to/leave.bpmn20.xml")
                .deploy();

// 启动名为 'leave' 的流程实例
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("leave");
    }


}
