package com.qiujie.controller;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Attendance;
import com.qiujie.service.AttendanceService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @ApiOperation("新增")
    @PostMapping
    public ResponseDTO add(@RequestBody Attendance attendance) {
        return this.attendanceService.add(attendance);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.attendanceService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.attendanceService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    public ResponseDTO edit(@RequestBody Attendance attendance) {
        return this.attendanceService.edit(attendance);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.attendanceService.query(id);
    }

    @ApiOperation("分页查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('performance:attendance:list','performance:attendance:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String name,
                            Integer deptId,
                            String month) {
        return this.attendanceService.list(current, size, name, deptId, month);
    }

    @ApiOperation("同步导出")
    @GetMapping("/export/{month}/{filename}")
    @PreAuthorize("hasAnyAuthority('performance:attendance:export')")
    public void export(HttpServletResponse response, @PathVariable String month, @PathVariable String filename) throws IOException {
        this.attendanceService.export(response, month, filename);
    }

    @ApiOperation("同步导入")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('performance:attendance:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.attendanceService.imp(file);
    }

    @ApiOperation("创建导入任务")
    @PostMapping("/import/task")
    @PreAuthorize("hasAnyAuthority('performance:attendance:import')")
    public ResponseDTO createImportTask(MultipartFile file) throws IOException {
        return this.attendanceService.createImportTask(file);
    }

    @ApiOperation("创建导出任务")
    @PostMapping("/export/task")
    @PreAuthorize("hasAnyAuthority('performance:attendance:export')")
    public ResponseDTO createExportTask(@RequestParam(required = false) String month,
                                        @RequestParam(required = false) String filename) {
        return this.attendanceService.createExportTask(month, filename);
    }

    @ApiOperation("按员工和日期查询")
    @GetMapping("/{id}/{date}")
    public ResponseDTO queryByStaffIdAndDate(@PathVariable Integer id, @PathVariable String date) {
        return this.attendanceService.queryByStaffIdAndDate(id, date);
    }

    @ApiOperation("保存或更新")
    @PutMapping("/set")
    @PreAuthorize("hasAnyAuthority('performance:attendance:set')")
    public ResponseDTO setAttendance(@RequestBody Attendance attendance) {
        return this.attendanceService.setAttendance(attendance);
    }

    @ApiOperation("查询全部状态")
    @GetMapping("/all")
    public ResponseDTO queryAll() {
        return this.attendanceService.queryAll();
    }
}
