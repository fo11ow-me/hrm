package com.qiujie.controller;

import com.qiujie.entity.Role;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.RoleMenuService;
import com.qiujie.service.RoleService;
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
 * @since 2022-02-28
 */
@RestController
@RequestMapping("/role")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleMenuService roleMenuService;

    @ApiOperation("新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('permission:role:add')")
    public ResponseDTO add(@RequestBody Role role) {
        return this.roleService.add(role);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('permission:role:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.roleService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('permission:role:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.roleService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('permission:role:edit')")
    public ResponseDTO edit(@RequestBody Role role) {
        return this.roleService.edit(role);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.roleService.query(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/all")
    public ResponseDTO queryAll(){
        return this.roleService.queryAll();
    }

    @ApiOperation("分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('permission:role:list','permission:role:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name) {
        return this.roleService.list(current, size, name);
    }

    @ApiOperation("数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('permission:role:export')")
    public void export(HttpServletResponse response,@PathVariable  String filename) throws IOException {
         this.roleService.export(response,filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('permission:role:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.roleService.imp(file);
    }

    @ApiOperation("为角色设置菜单")
    @PostMapping("/set/{id}")
    @PreAuthorize("hasAnyAuthority('permission:role:set_menu')")
    public ResponseDTO setMenu(@PathVariable Integer id, @RequestBody List<Integer> menuIds){
        return this.roleMenuService.setMenu(id, menuIds);
    }

    @ApiOperation("得到角色所属的菜单")
    @GetMapping("/role/{id}")
    public ResponseDTO queryByRoleId(@PathVariable Integer id){
        return this.roleMenuService.queryByRoleId(id);
    }
}

