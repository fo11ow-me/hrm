package com.qiujie.controller;

import com.qiujie.entity.Overtime;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.service.OvertimeService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * <p>
 * 加班表 前端控制器
 * </p>
 *
 * @author qiujie
 * @since 2022-03-28
 */
@RestController
@RequestMapping("/overtime")
public class OvertimeController {
    @Autowired
    private OvertimeService overtimeService;

    @ApiOperation("新增")
    @PostMapping
    public ResponseDTO add(@RequestBody Overtime overtime) {
        return this.overtimeService.add(overtime);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.overtimeService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.overtimeService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    public ResponseDTO edit(@RequestBody Overtime overtime) {
        return this.overtimeService.edit(overtime);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.overtimeService.query(id);
    }

    @ApiOperation("获取")
    @GetMapping("/{deptId}/{typeNum}")
    public ResponseDTO queryByDeptIdAndTypeNum(@PathVariable Integer deptId, @PathVariable String typeNum) {
        return this.overtimeService.queryByDeptIdAndTypeNum(deptId,typeNum);
    }

    @ApiOperation("设置加班")
    @PostMapping("/set")
    @PreAuthorize("hasAnyAuthority('system:department:setting')")
    public ResponseDTO setOvertime(@RequestBody Overtime overtime) {
        return this.overtimeService.setOvertime(overtime);
    }

    @ApiOperation("获取所有")
    @GetMapping("/all")
    public ResponseDTO queryAll() {
        return this.overtimeService.queryAll();
    }

}

