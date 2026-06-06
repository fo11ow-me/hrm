package com.qiujie.controller;

import com.qiujie.entity.SalaryDeduct;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.SalaryDeductService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * <p>
 * 工资扣除表 前端控制器
 * </p>
 *
 * @author qiujie
 * @since 2022-03-27
 */
@RestController
@RequestMapping("/salary-deduct")
public class SalaryDeductController {
    @Autowired
    private SalaryDeductService salaryDeductService;

    @ApiOperation("新增")
    @PostMapping
    public ResponseDTO add(@RequestBody SalaryDeduct salaryDeduct) {
        return this.salaryDeductService.add(salaryDeduct);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.salaryDeductService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.salaryDeductService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    public ResponseDTO edit(@RequestBody SalaryDeduct salaryDeduct) {
        return this.salaryDeductService.edit(salaryDeduct);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.salaryDeductService.query(id);
    }

    @ApiOperation("获取")
    @GetMapping("/{deptId}/{typeNum}")
    public ResponseDTO queryByDeptIdAndTypeNum(@PathVariable Integer deptId, @PathVariable Integer typeNum) {
        return this.salaryDeductService.queryByDeptIdAndTypeNum(deptId, typeNum);
    }

    @ApiOperation("设置罚款")
    @PostMapping("/set")
    @PreAuthorize("hasAnyAuthority('system:department:setting')")
    public ResponseDTO setSalaryDeduct(@RequestBody SalaryDeduct salaryDeduct) {
        return this.salaryDeductService.setSalaryDeduct(salaryDeduct);
    }

    @ApiOperation("获取所有")
    @GetMapping("/all")
    public ResponseDTO queryAll() {
        return this.salaryDeductService.queryAll();
    }

}

