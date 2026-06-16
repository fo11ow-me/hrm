package com.qiujie.controller;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Attendance;
import com.qiujie.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Operation(summary = "新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('performance:attendance:add')")
    public ResponseDTO add(@RequestBody Attendance attendance) {
        return this.attendanceService.add(attendance);
    }

    @Operation(summary = "逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('performance:attendance:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.attendanceService.delete(id);
    }

    @Operation(summary = "批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('performance:attendance:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.attendanceService.deleteBatch(ids);
    }

    @Operation(summary = "编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('performance:attendance:edit')")
    public ResponseDTO edit(@RequestBody Attendance attendance) {
        return this.attendanceService.edit(attendance);
    }

    @Operation(summary = "查询单条")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('performance:attendance:query')")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.attendanceService.query(id);
    }

    @Operation(summary = "分页查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('performance:attendance:list','performance:attendance:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String name,
                            Integer deptId,
                            String month) {
        return this.attendanceService.list(current, size, name, deptId, month);
    }

    @Operation(summary = "同步导出")
    @GetMapping("/export/{month}/{filename}")
    @PreAuthorize("hasAnyAuthority('performance:attendance:export')")
    public void export(HttpServletResponse response, @PathVariable String month, @PathVariable String filename) throws IOException {
        this.attendanceService.export(response, month, filename);
    }

    @Operation(summary = "同步导入")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('performance:attendance:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.attendanceService.imp(file);
    }

    @Operation(summary = "创建导入任务")
    @PostMapping("/import/task")
    @PreAuthorize("hasAnyAuthority('performance:attendance:import')")
    public ResponseDTO createImportTask(@RequestParam String uploadId) {
        return this.attendanceService.createImportTask(uploadId);
    }

    @Operation(summary = "创建导出任务")
    @PostMapping("/export/task")
    @PreAuthorize("hasAnyAuthority('performance:attendance:export')")
    public ResponseDTO createExportTask(@RequestParam(required = false) String month,
                                        @RequestParam(required = false) String filename) {
        return this.attendanceService.createExportTask(month, filename);
    }

    @Operation(summary = "按员工和日期查询")
    @GetMapping("/{id}/{date}")
    @PreAuthorize("hasAnyAuthority('performance:attendance:list','performance:attendance:search')")
    public ResponseDTO queryByStaffIdAndDate(@PathVariable Integer id, @PathVariable String date) {
        return this.attendanceService.queryByStaffIdAndDate(id, date);
    }

    @Operation(summary = "保存或更新")
    @PutMapping("/set")
    @PreAuthorize("hasAnyAuthority('performance:attendance:set')")
    public ResponseDTO setAttendance(@RequestBody Attendance attendance) {
        return this.attendanceService.setAttendance(attendance);
    }

    @Operation(summary = "查询全部状态")
    @GetMapping("/all")
    public ResponseDTO queryAll() {
        return this.attendanceService.queryAll();
    }
}
