package com.qiujie.controller;


import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.StaffOvertime;
import com.qiujie.service.StaffOvertimeService;
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
 *
 * @author qiujie
 * @since 2024-03-20
 */
@RestController
@RequestMapping("/staff-overtime")
public class StaffOvertimeController {
    @Autowired
    private StaffOvertimeService staffOvertimeService;

    @Operation(summary = "新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('performance:overtime:add')")
    public ResponseDTO add(@RequestBody StaffOvertime staffOvertime) {
        return this.staffOvertimeService.add(staffOvertime);
    }

    @Operation(summary = "逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('performance:overtime:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.staffOvertimeService.delete(id);
    }

    @Operation(summary = "批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('performance:overtime:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.staffOvertimeService.deleteBatch(ids);
    }

    @Operation(summary = "编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('performance:overtime:edit')")
    public ResponseDTO edit(@RequestBody StaffOvertime staffOvertime) {
        return this.staffOvertimeService.edit(staffOvertime);
    }

    @Operation(summary = "查询")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('performance:overtime:query')")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.staffOvertimeService.query(id);
    }

    @Operation(summary = "分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('performance:overtime:list','performance:overtime:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name, Integer deptId,String month) {
        return this.staffOvertimeService.list(current, size, name,deptId ,month);
    }

    @Operation(summary = "数据导出接口")
    @GetMapping("/export/{month}/{filename}")
    @PreAuthorize("hasAnyAuthority('performance:overtime:export')")
    public void export(HttpServletResponse response, @PathVariable String month,@PathVariable String filename) throws IOException {
         this.staffOvertimeService.export(response, month,filename);
    }

    @Operation(summary = "数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('performance:overtime:import')")

    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.staffOvertimeService.imp(file);
    }

    @Operation(summary = "异步导入")
    @PostMapping("/import/task")
    @PreAuthorize("hasAnyAuthority('performance:overtime:import')")
    public ResponseDTO createImportTask(@RequestParam String uploadId) {
        return this.staffOvertimeService.createImportTask(uploadId);
    }

    @Operation(summary = "异步导出")
    @GetMapping("/export/task")
    @PreAuthorize("hasAnyAuthority('performance:overtime:export')")
    public ResponseDTO createExportTask(@RequestParam String month, @RequestParam String filename) {
        return this.staffOvertimeService.createExportTask(month, filename);
    }

    @Operation(summary = "查询")
    @GetMapping("/{id}/{date}")
    @PreAuthorize("hasAnyAuthority('performance:overtime:query')")
    public ResponseDTO queryByStaffIdAndDate(@PathVariable Integer id,@PathVariable String date) {
        return this.staffOvertimeService.queryByStaffIdAndDate(id,date);
    }


    @Operation(summary = "设置加班")
    @PostMapping("/set")
    @PreAuthorize("hasAnyAuthority('performance:overtime:set')")
    public ResponseDTO setOvertime(@RequestBody StaffOvertime staffOvertime) {
        return this.staffOvertimeService.setOvertime(staffOvertime);
    }

    @Operation(summary = "查询调休天数（所有员工均可使用）")
    @GetMapping("/time/off/{id}")
    public ResponseDTO queryTimeOffDaysByStaffId(@PathVariable Integer id) {
        return this.staffOvertimeService.queryTimeOffDaysByStaffId(id);
    }

}

