package com.qiujie.controller;


import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.StaffOvertime;
import com.qiujie.service.StaffOvertimeService;
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
 *
 * @author qiujie
 * @since 2024-03-20
 */
@RestController
@RequestMapping("/staff-overtime")
public class StaffOvertimeController {
    @Autowired
    private StaffOvertimeService staffOvertimeService;

    @ApiOperation("新增")
    @PostMapping
    public ResponseDTO add(@RequestBody StaffOvertime staffOvertime) {
        return this.staffOvertimeService.add(staffOvertime);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.staffOvertimeService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.staffOvertimeService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    public ResponseDTO edit(@RequestBody StaffOvertime staffOvertime) {
        return this.staffOvertimeService.edit(staffOvertime);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.staffOvertimeService.query(id);
    }

    @ApiOperation("分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('performance:overtime:list','performance:overtime:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name, Integer deptId,String month) {
        return this.staffOvertimeService.list(current, size, name,deptId ,month);
    }

    @ApiOperation("数据导出接口")
    @GetMapping("/export/{month}/{filename}")
    @PreAuthorize("hasAnyAuthority('performance:overtime:export')")
    public void export(HttpServletResponse response, @PathVariable String month,@PathVariable String filename) throws IOException {
         this.staffOvertimeService.export(response, month,filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('performance:overtime:import')")

    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.staffOvertimeService.imp(file);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}/{date}")
    public ResponseDTO queryByStaffIdAndDate(@PathVariable Integer id,@PathVariable String date) {
        return this.staffOvertimeService.queryByStaffIdAndDate(id,date);
    }


    @ApiOperation("设置加班")
    @PostMapping("/set")
    @PreAuthorize("hasAnyAuthority('performance:overtime:set')")
    public ResponseDTO setOvertime(@RequestBody StaffOvertime staffOvertime) {
        return this.staffOvertimeService.setOvertime(staffOvertime);
    }

    @ApiOperation("查询")
    @GetMapping("/time/off/{id}")
    public ResponseDTO queryTimeOffDaysByStaffId(@PathVariable Integer id) {
        return this.staffOvertimeService.queryTimeOffDaysByStaffId(id);
    }

}

