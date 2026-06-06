package com.qiujie.controller;

import com.qiujie.entity.Leave;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.LeaveService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * <p>
 * 请假表 前端控制器
 * </p>
 *
 * @author qiujie
 * @since 2022-03-27
 */
@RestController
@RequestMapping("/leave")
public class LeaveController {
    @Autowired
    private LeaveService leaveService;

    @ApiOperation("新增")
    @PostMapping
    public ResponseDTO add(@RequestBody Leave leave) {
        return this.leaveService.add(leave);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.leaveService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.leaveService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    public ResponseDTO edit(@RequestBody Leave leave) {
        return this.leaveService.edit(leave);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.leaveService.query(id);
    }


    @ApiOperation("获取")
    @GetMapping("/{deptId}/{typeNum}")
    public ResponseDTO queryByDeptIdAndTypeNum(@PathVariable Integer deptId, @PathVariable Integer typeNum) {
        return this.leaveService.queryByDeptIdAndTypeNum(deptId, typeNum);
    }

    @ApiOperation("设置假期")
    @PostMapping("/set")
    @PreAuthorize("hasAnyAuthority('system:department:setting')")
    public ResponseDTO setLeave(@RequestBody Leave leave) {
        return this.leaveService.setLeave(leave);
    }


    @ApiOperation("查询")
    @GetMapping("/dept/{id}")
    public ResponseDTO queryByDeptId(@PathVariable Integer id) {
        return this.leaveService.queryByDeptId(id);
    }

    @ApiOperation("获取所有")
    @GetMapping("/all")
    public ResponseDTO queryAll() {
        return this.leaveService.queryAll();
    }

}

