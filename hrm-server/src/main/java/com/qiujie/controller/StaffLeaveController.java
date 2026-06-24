package com.qiujie.controller;

import com.qiujie.service.StaffLeaveService;
import com.qiujie.util.SecurityUtil;
import com.qiujie.dto.Response;
import com.qiujie.entity.StaffLeave;

import com.qiujie.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author qiujie
 * @since 2022-04-05
 */
@RestController
@RequestMapping("/staff-leave")
public class StaffLeaveController {

    @Autowired
    private StaffLeaveService staffLeaveService;
    @Autowired
    private SecurityUtil securityUtil;

    @Operation(summary = "新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('performance:leave:add')")
    public ResponseDTO add(@RequestBody StaffLeave staffLeave) {
        return this.staffLeaveService.add(staffLeave);
    }

    @Operation(summary = "逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('performance:leave:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.staffLeaveService.delete(id);
    }

    @Operation(summary = "批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('performance:leave:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.staffLeaveService.deleteBatch(ids);
    }

    @Operation(summary = "编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('performance:leave:edit')")
    public ResponseDTO edit(@RequestBody StaffLeave staffLeave) {
        return this.staffLeaveService.edit(staffLeave);
    }


    @Operation(summary = "查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.staffLeaveService.query(id);
    }


    /**
     *
     * @param current
     * @param size
     * @param name
     * @param deptId
     * @param code 用户工号
     * @return
     */
    @Operation(summary = "分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('performance:leave:list','performance:leave:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name, Integer deptId,String code) {
        return this.staffLeaveService.list(current, size, name,deptId,code);
    }

    @Operation(summary = "数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('performance:leave:export')")
    public void export(HttpServletResponse response,@PathVariable  String filename) throws IOException {
         this.staffLeaveService.export(response,filename);
    }

    @Operation(summary = "数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('performance:leave:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.staffLeaveService.imp(file);
    }

    @Operation(summary = "分页")
    @GetMapping("/staff")
    @PreAuthorize("hasAnyAuthority('performance:leave:list','performance:leave:search')")
    public ResponseDTO queryByStaffId(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, Integer id) {
        // 仅允许查自己，或具备 leave:list 权限的管理员
        Integer currentStaffId = securityUtil.getCurrentOperatorId();
        if (!currentStaffId.equals(id)) {
            return Response.error(1300, "无权限查看他人请假记录");
        }
        return this.staffLeaveService.queryByStaffId(current, size, id);
    }

    @Operation(summary = "获取所有")
    @GetMapping("/all")
    public ResponseDTO queryAll() {
        return this.staffLeaveService.queryAll();
    }

    @Operation(summary = "申请请假")
    @PostMapping("/apply")
    public ResponseDTO apply(@RequestBody StaffLeave staffLeave) {
        return this.staffLeaveService.apply(staffLeave);
    }

    @Operation(summary = "拾取请假任务")
    @PostMapping("/claim")
    @PreAuthorize("hasAnyAuthority('performance:leave:claim')")
    public ResponseDTO claim(@RequestBody StaffLeave staffLeave) {
        return this.staffLeaveService.claim(staffLeave);
    }

    @Operation(summary = "归还请假任务")
    @PostMapping("/revert")
    @PreAuthorize("hasAnyAuthority('performance:leave:claim')")
    public ResponseDTO revert(@RequestBody StaffLeave staffLeave) {
        return this.staffLeaveService.revert(staffLeave);
    }


    @Operation(summary = "完成任务")
    @PostMapping("/complete")
    @PreAuthorize("hasAnyAuthority('performance:leave:approve','performance:leave:reject')")
    public ResponseDTO complete(@RequestBody StaffLeave staffLeave) {
        return this.staffLeaveService.complete(staffLeave);
    }

    @Operation(summary = "撤销请假")
    @PostMapping("/cancel")
    public ResponseDTO cancel(@RequestBody StaffLeave staffLeave){
        return this.staffLeaveService.cancel(staffLeave);
    }
}

