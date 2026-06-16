package com.qiujie.controller;

import com.qiujie.entity.Staff;
import com.qiujie.service.InsuranceService;
import com.qiujie.entity.Insurance;

import com.qiujie.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author qiujie
 * @since 2022-03-23
 */
@RestController
@RequestMapping("/insurance")
public class InsuranceController {
    @Autowired
    private InsuranceService insuranceService;

    @Operation(summary = "新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('money:insurance:add')")
    public ResponseDTO add(@RequestBody Insurance insurance) {
        return this.insuranceService.add(insurance);
    }

    @Operation(summary = "逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('money:insurance:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.insuranceService.delete(id);
    }

    @Operation(summary = "批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('money:insurance:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.insuranceService.deleteBatch(ids);
    }

    @Operation(summary = "编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('money:insurance:edit')")
    public ResponseDTO edit(@RequestBody Insurance insurance) {
        return this.insuranceService.edit(insurance);
    }

    @Operation(summary = "查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.insuranceService.query(id);
    }

    @Operation(summary = "查询")
    @GetMapping("/staff/{id}")
    public ResponseDTO queryByStaffId(@PathVariable Integer id) {
        return this.insuranceService.queryByStaffId(id);
    }

    @Operation(summary = "多条件分页查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('money:insurance:list','money:insurance:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name, Integer deptId){
        return this.insuranceService.list(current, size, name,deptId);
    }

    @Operation(summary = "数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('money:insurance:export')")
    public void export(HttpServletResponse response,@PathVariable  String filename) throws IOException {
         this.insuranceService.export(response,filename);
    }

    @Operation(summary = "数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('money:insurance:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.insuranceService.imp(file);
    }

    @Operation(summary = "为员工设置社保")
    @PostMapping("/set")
    @PreAuthorize("hasAnyAuthority('money:insurance:set')")
    public ResponseDTO setInsurance(@RequestBody Insurance insurance) {
        return this.insuranceService.setInsurance(insurance);
    }
}

