package com.qiujie.controller;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Menu;
import com.qiujie.service.MenuService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
@RequestMapping("/menu")
public class MenuController {
    @Autowired
    private MenuService menuService;

    @ApiOperation("新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('permission:menu:add')")
    public ResponseDTO add(@RequestBody Menu menu) {
        return this.menuService.add(menu);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('permission:menu:delete')")
    public ResponseDTO deleteById(@PathVariable Integer id) {
        return this.menuService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('permission:menu:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.menuService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('permission:menu:edit','permission:menu:enable')")
    public ResponseDTO edit(@RequestBody Menu menu) {
        return this.menuService.edit(menu);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.menuService.query(id);
    }



    @ApiOperation("分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('permission:menu:list','permission:menu:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name) {
        return this.menuService.list(current, size, name);
    }

    @ApiOperation("数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('permission:menu:export')")
    public void export(HttpServletResponse response,@PathVariable  String filename) throws IOException {
         this.menuService.export(response,filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('permission:menu:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.menuService.imp(file);
    }

    @ApiOperation("查询所有")
    @GetMapping("/all")
    public ResponseDTO queryAll(){
        return this.menuService.queryAll();
    }

    @ApiOperation("获取员工的菜单")
    @GetMapping("/staff/{id}")
    public ResponseDTO queryByStaffId(@PathVariable Integer id){
        return this.menuService.queryByStaffId(id);
    }

    @ApiOperation("查询权限")
    @GetMapping("/permission/{id}")
    public ResponseDTO queryPermission(@PathVariable Integer id){
        return this.menuService.queryPermission(id);
    }
}

