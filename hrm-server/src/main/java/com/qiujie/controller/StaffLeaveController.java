package com.qiujie.controller;

import com.qiujie.service.StaffLeaveService;
import com.qiujie.entity.StaffLeave;

import com.qiujie.dto.ResponseDTO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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

    @ApiOperation("新增")
    @PostMapping
    public ResponseDTO add(@RequestBody StaffLeave staffLeave) {
        return this.staffLeaveService.add(staffLeave);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.staffLeaveService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.staffLeaveService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    public ResponseDTO edit(@RequestBody StaffLeave staffLeave) {
        return this.staffLeaveService.edit(staffLeave);
    }


    @ApiOperation("查询")
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
    @ApiOperation("分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('performance:leave:list','performance:leave:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name, Integer deptId,String code) {
        return this.staffLeaveService.list(current, size, name,deptId,code);
    }

    @ApiOperation("数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('performance:leave:export')")
    public void export(HttpServletResponse response,@PathVariable  String filename) throws IOException {
         this.staffLeaveService.export(response,filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('performance:leave:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.staffLeaveService.imp(file);
    }

    @ApiOperation("分页")
    @GetMapping("/staff")
    public ResponseDTO queryByStaffId(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, Integer id) {
        return this.staffLeaveService.queryByStaffId(current, size, id);
    }

    @ApiOperation("获取所有")
    @GetMapping("/all")
    public ResponseDTO queryAll() {
        return this.staffLeaveService.queryAll();
    }

    @ApiOperation("申请请假")
    @PostMapping("/apply/{code}")
    public ResponseDTO apply(@RequestBody StaffLeave staffLeave,@PathVariable String code) {
        return this.staffLeaveService.apply(staffLeave,code);
    }

    @ApiOperation("拾取请假任务")
    @PostMapping("/claim/{code}")
    @PreAuthorize("hasAnyAuthority('performance:leave:claim')")
    public ResponseDTO claim(@RequestBody StaffLeave staffLeave,@PathVariable String code) {
        return this.staffLeaveService.claim(staffLeave,code);
    }

    @ApiOperation("归还请假任务")
    @PostMapping("/revert/{code}")
    public ResponseDTO revert(@RequestBody StaffLeave staffLeave,@PathVariable String code) {
        return this.staffLeaveService.revert(staffLeave,code);
    }


    @ApiOperation("完成任务")
    @PostMapping("/complete/{code}")
    public ResponseDTO complete(@RequestBody StaffLeave staffLeave, @PathVariable String code) {
        return this.staffLeaveService.complete(staffLeave,code);
    }

    @ApiOperation("撤销请假")
    @PostMapping("/cancel")
    public ResponseDTO cancel(@RequestBody StaffLeave staffLeave){
        return this.staffLeaveService.cancel(staffLeave);
    }
}

