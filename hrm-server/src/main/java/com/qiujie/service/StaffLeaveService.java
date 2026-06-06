package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Staff;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.AuditStatusEnum;
import com.qiujie.mapper.StaffLeaveMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.EnumUtil;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.vo.StaffLeaveVO;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-04-05
 */
@Service
public class StaffLeaveService extends ServiceImpl<StaffLeaveMapper, StaffLeave> {

    @Autowired
    private StaffLeaveMapper staffLeaveMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    public ResponseDTO add(StaffLeave staffLeave) {
        if (save(staffLeave)) {
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


    @Transactional
    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * @param staffLeave
     * @return
     */
    public ResponseDTO edit(StaffLeave staffLeave) {
        if (updateById(staffLeave)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        StaffLeave staffLeave = getById(id);
        if (staffLeave != null) {
            return Response.success(staffLeave);
        }
        return Response.error();
    }


    public ResponseDTO list(Integer current, Integer size, String name, Integer deptId, String code) {
        IPage<StaffLeaveVO> config = new Page<>(current, size);
        // 查询当前用户的组任务以及个人任务
        List<Task> taskList = this.taskService.createTaskQuery().processDefinitionKey("leave").taskCandidateOrAssigned(code).list();
        List<Integer> ids = new ArrayList<>();
        for (Task task : taskList) {
            if (task != null) {
                ProcessInstance instance = this.runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                ids.add(Integer.valueOf(instance.getBusinessKey()));
            }
        }
        IPage<StaffLeaveVO> page = this.staffLeaveMapper.listStaffLeaveVO(config, name, deptId, ids);
        List<StaffLeaveVO> staffLeaveVOList = page.getRecords();
        List<HashMap<String, Object>> list = new ArrayList<>();
        for (StaffLeaveVO staffLeaveVO : staffLeaveVOList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("staffLeave", staffLeaveVO);
            map.put("tagType", staffLeaveVO.getStatus().getTagType());
            map.put("approve", AuditStatusEnum.APPROVE);
            map.put("reject", AuditStatusEnum.REJECT);
            map.put("unaudited", AuditStatusEnum.UNAUDITED);
            map.put("auditing", AuditStatusEnum.AUDITING);
            list.add(map);
        }
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", list);
        return Response.success(map);
    }

    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public void export(HttpServletResponse response, String filename) throws IOException {
        List<StaffLeave> list = list();
        EasyExcelUtil.write(response, list, filename, StaffLeave.class);
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
        List<StaffLeave> list = EasyExcelUtil.read(inputStream, 1, StaffLeave.class);
        // IService接口中的方法.批量插入数据
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO queryByStaffId(Integer current, Integer size, Integer id) {
        IPage<StaffLeave> config = new Page<>(current, size);
        IPage<StaffLeave> page = this.staffLeaveMapper.listStaffLeaveByStaffId(config, id);
        List<StaffLeave> records = page.getRecords();
        List<HashMap<String, Object>> list = new ArrayList<>();
        for (StaffLeave staffLeave : records) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("staffLeave", staffLeave);
            map.put("tagType", staffLeave.getStatus().getTagType());
            map.put("unaudited", AuditStatusEnum.UNAUDITED);
            map.put("approve", AuditStatusEnum.APPROVE);
            map.put("reject", AuditStatusEnum.REJECT);
            map.put("cancel", AuditStatusEnum.CANCEL);
            list.add(map);
        }
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", list);
        return Response.success(map);
    }

    public ResponseDTO queryAll() {
        List<Map<String, Object>> enumList = EnumUtil.getEnumList(AuditStatusEnum.class);
        for (Map<String, Object> map : enumList) {
            for (AuditStatusEnum auditStatusEnum : AuditStatusEnum.values()) {
                if (map.get("code") == auditStatusEnum.getCode()) {
                    map.put("tagType", auditStatusEnum.getTagType());
                }
            }
        }
        return Response.success(enumList);
    }

    /**
     * 请假
     *
     * @param staffLeave
     * @param code       工号
     * @return
     */
    @Transactional
    public ResponseDTO apply(StaffLeave staffLeave, String code) {
        List<StaffLeave> staffLeaveList = this.staffLeaveMapper.selectList(new QueryWrapper<StaffLeave>().eq("staff_id", staffLeave.getStaffId())
                .and(i -> i
                        .eq("status", AuditStatusEnum.UNAUDITED).or()
                        .eq("status", AuditStatusEnum.REJECT).or()
                        .eq("status", AuditStatusEnum.AUDITING))
        );
        if (!staffLeaveList.isEmpty()) {
            return Response.error("你有待审核、被驳回、正在审核中的请假申请！");
        }
        if (!save(staffLeave)) {
            return Response.error("提交失败！");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("staff", code);
        this.runtimeService.startProcessInstanceByKey("leave", String.valueOf(staffLeave.getId()), map);
        Task task = this.taskService.createTaskQuery().processDefinitionKey("leave")
                .processInstanceBusinessKey(String.valueOf(staffLeave.getId()))
                .taskAssignee(code).singleResult();
        if (task != null) {
            List<Staff> staffList = this.staffMapper.queryByRole("hr");
            Map<String, Object> map1 = new HashMap<>();
            map1.put("hr", staffList.stream().map(Staff::getCode).collect(Collectors.joining(",")));
            // 完成任务
            taskService.complete(task.getId(), map1);
        }
        return Response.success();
    }

    /**
     * 拾取请假申请
     *
     * @param staffLeave
     * @param code       工号
     * @return
     */
    @Transactional
    public ResponseDTO claim(StaffLeave staffLeave, String code) {
        if (!updateById(staffLeave)) {
            return Response.error();
        }
        Task task = this.taskService.createTaskQuery().processDefinitionKey("leave")
                .processInstanceBusinessKey(staffLeave.getId().toString())
                .taskCandidateUser(code).singleResult();
        if (task == null) {
            return Response.error();
        }
        this.taskService.claim(task.getId(), code);
        return Response.success();
    }


    /**
     * 归还请假任务
     *
     * @param staffLeave
     * @param code       工号
     * @return
     */
    @Transactional
    public ResponseDTO revert(StaffLeave staffLeave, String code) {
        if (!updateById(staffLeave)) {
            return Response.error();
        }
        Task task = this.taskService.createTaskQuery().processDefinitionKey("leave")
                .processInstanceBusinessKey(staffLeave.getId().toString())
                .taskAssignee(code).singleResult();
        if (task == null) {
            return Response.error();
        }
        this.taskService.setAssignee(task.getId(), null); // userId不能为""
        return Response.success();
    }


    /**
     * 完成任务
     *
     * @param staffLeave
     * @param code
     * @return
     */
    @Transactional
    public ResponseDTO complete(StaffLeave staffLeave, String code) {
        if (!updateById(staffLeave)) {
            return Response.error();
        }
        Task task = this.taskService.createTaskQuery().processDefinitionKey("leave")
                .processInstanceBusinessKey(staffLeave.getId().toString())
                .taskAssignee(code).singleResult();
        if (task != null) {
            Map<String, Object> map = new HashMap<>();
            if (Objects.equals(task.getTaskDefinitionKey(), "hr_audit")) {
                map.put("hrAuditStatus", staffLeave.getStatus().getCode());
            } else if(Objects.equals(task.getTaskDefinitionKey(), "manager_audit")) {
                map.put("managerAuditStatus", staffLeave.getStatus().getCode());
            } else{
                map = null;
            }
            taskService.complete(task.getId(), null, map);
            return Response.success();
        }
        return Response.error();
    }


    /**
     * 撤销请假申请
     *
     * @param staffLeave
     * @return
     */
    @Transactional
    public ResponseDTO cancel(StaffLeave staffLeave) {
        if (!updateById(staffLeave)) {
            return Response.error();
        }
        ProcessInstance instance = this.runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("leave")
                .processInstanceBusinessKey(staffLeave.getId().toString()).singleResult();
        if (instance != null) {
            runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), staffLeave.getStatus().getMessage());
            return Response.success();
        }
        return Response.error();
    }

}




