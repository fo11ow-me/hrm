package com.qiujie.controller;


import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Staff;
import com.qiujie.service.StaffRoleService;
import com.qiujie.service.StaffService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author qiujie
 * @since 2022-01-27
 */
@RestController
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private StaffService staffService;

    @Autowired
    private StaffRoleService staffRoleService;

    @ApiOperation("新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:staff:add')")
    public ResponseDTO add(@RequestBody Staff staff) {
        return this.staffService.add(staff);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:staff:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.staffService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('system:staff:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.staffService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:staff:edit','system:staff:enable')")
    public ResponseDTO edit(@RequestBody Staff staff) {
        return this.staffService.edit(staff);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.staffService.query(id);
    }

    @ApiOperation("查询员工信息")
    @GetMapping("/info/{id}")
    public ResponseDTO queryInfo(@PathVariable Integer id) {
        return this.staffService.queryInfo(id);
    }

    @ApiOperation("多条件分页查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:staff:list','system:staff:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name, String birthday, Integer deptId, Integer status) {
        return this.staffService.list(current, size, name, birthday, deptId, status);
    }

    @ApiOperation("数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('system:staff:export')")
    public void export(HttpServletResponse response, @PathVariable String filename) throws IOException {
        this.staffService.export(response, filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('system:staff:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.staffService.imp(file);
    }

    @ApiOperation("为员工设置角色")
    @PostMapping("/set/{id}")
    @PreAuthorize("hasAnyAuthority('system:staff:set_role')")
    public ResponseDTO setRole(@PathVariable Integer id, @RequestBody List<Integer> roleIds) {
        return this.staffRoleService.setRole(id, roleIds);
    }

    @ApiOperation("得到员工的角色")
    @GetMapping("/staff/{id}")
    public ResponseDTO queryByStaffId(@PathVariable Integer id) {
        return this.staffRoleService.queryByStaffId(id);
    }


    @ApiOperation("检查员工的密码")
    @GetMapping("/{pwd}/{id}")
    public ResponseDTO validate(@PathVariable String pwd, @PathVariable Integer id) {
        return this.staffService.validate(pwd, id);
    }

    @ApiOperation("更新密码")
    @PutMapping("/reset")
    public ResponseDTO reset(@RequestBody Staff staff) {
        return this.staffService.reset(staff);
    }
}

