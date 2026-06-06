package com.qiujie.controller;

import com.qiujie.entity.Salary;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.SalaryService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author qiujie
 * @since 2022-04-06
 */
@RestController
@RequestMapping("/salary")
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    @ApiOperation("新增")
    @PostMapping
    public ResponseDTO add(@RequestBody Salary salary) {
        return this.salaryService.add(salary);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.salaryService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.salaryService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    public ResponseDTO edit(@RequestBody Salary salary) {
        return this.salaryService.edit(salary);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.salaryService.query(id);
    }

    @ApiOperation("分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('money:salary:list','money:salary:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name, Integer deptId, String month) {
        return this.salaryService.list(current, size, name, deptId, month);
    }

    @ApiOperation("数据导出接口")
    @GetMapping("/export/{month}/{filename}")
    @PreAuthorize("hasAnyAuthority('money:salary:export')")
    public void export(HttpServletResponse response, @PathVariable String month,@PathVariable  String filename) throws IOException {
         this.salaryService.export(response, month,filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('money:salary:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.salaryService.imp(file);
    }

    @ApiOperation("设置工资")
    @PostMapping("/set")
    @PreAuthorize("hasAnyAuthority('money:salary:set')")
    public ResponseDTO setSalary(@RequestBody Salary salary) {
        return this.salaryService.setSalary(salary);
    }


}

