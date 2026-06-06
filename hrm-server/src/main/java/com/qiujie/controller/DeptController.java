package com.qiujie.controller;

import com.qiujie.entity.Dept;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.DeptService;
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
 * @since 2022-03-07
 */
@RestController
@RequestMapping("/dept")
public class DeptController {
    @Autowired
    private DeptService deptService;

    @ApiOperation("新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:department:add')")
    public ResponseDTO add(@RequestBody Dept dept) {
        return this.deptService.add(dept);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:department:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.deptService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('system:department:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.deptService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:department:edit')")
    public ResponseDTO edit(@RequestBody Dept dept) {
        return this.deptService.edit(dept);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.deptService.query(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/all")
    public ResponseDTO queryAll(){
        return this.deptService.queryAll();
    }

    @ApiOperation("条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:department:list','system:department:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name) {
        return this.deptService.list(current, size, name);
    }

    @ApiOperation("数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('system:department:export')")
    public void export(HttpServletResponse response,@PathVariable String filename) throws Exception {
         this.deptService.export(response,filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('system:department:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.deptService.imp(file);
    }

}

