package com.qiujie.controller;

import com.qiujie.entity.Salary;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.SalaryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;


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

    @Operation(summary = "新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('money:salary:add')")
    public ResponseDTO add(@RequestBody Salary salary) {
        return this.salaryService.add(salary);
    }

    @Operation(summary = "逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('money:salary:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.salaryService.delete(id);
    }

    @Operation(summary = "批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('money:salary:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.salaryService.deleteBatch(ids);
    }

    @Operation(summary = "编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('money:salary:edit')")
    public ResponseDTO edit(@RequestBody Salary salary) {
        return this.salaryService.edit(salary);
    }

    @Operation(summary = "查询")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('money:salary:query')")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.salaryService.query(id);
    }

    @Operation(summary = "分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('money:salary:list','money:salary:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name, Integer deptId, String month) {
        return this.salaryService.list(current, size, name, deptId, month);
    }

    @Operation(summary = "数据导出接口")
    @GetMapping("/export/{month}/{filename}")
    @PreAuthorize("hasAnyAuthority('money:salary:export')")
    public void export(HttpServletResponse response, @PathVariable String month,@PathVariable  String filename) throws IOException {
         this.salaryService.export(response, month,filename);
    }

    @Operation(summary = "数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('money:salary:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.salaryService.imp(file);
    }

    @Operation(summary = "通过上传会话创建异步导入任务")
    @PostMapping("/import/task")
    @PreAuthorize("hasAnyAuthority('money:salary:import')")
    public ResponseDTO createImportTask(@RequestParam String uploadId) {
        return this.salaryService.createImportTask(uploadId);
    }

    @Operation(summary = "异步导出接口（支持大文件）")
    @GetMapping("/export/task")
    @PreAuthorize("hasAnyAuthority('money:salary:export')")
    public ResponseDTO createExportTask(@RequestParam String month, @RequestParam String filename) {
        return this.salaryService.createExportTask(month, filename);
    }

    @Operation(summary = "设置工资")
    @PostMapping("/set")
    @PreAuthorize("hasAnyAuthority('money:salary:set')")
    public ResponseDTO setSalary(@RequestBody Salary salary) {
        return this.salaryService.setSalary(salary);
    }


}

