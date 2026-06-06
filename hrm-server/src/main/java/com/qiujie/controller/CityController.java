package com.qiujie.controller;

import com.qiujie.entity.City;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.CityService;
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
 * @since 2022-03-23
 */
@RestController
@RequestMapping("/city")
public class CityController {
    @Autowired
    private CityService cityService;

    @ApiOperation("新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('money:city:add')")
    public ResponseDTO add(@RequestBody City city) {
        return this.cityService.add(city);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('money:city:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.cityService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('money:city:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.cityService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('money:city:edit')")
    public ResponseDTO edit(@RequestBody City city) {
        return this.cityService.edit(city);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.cityService.query(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/all")
    public ResponseDTO queryAll() {
        return this.cityService.queryAll();
    }

    @ApiOperation("条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('money:city:list','money:city:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String name) {
        return this.cityService.list(current, size, name);
    }


    @ApiOperation("数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('money:city:export')")
    public void export(HttpServletResponse response, @PathVariable String filename) throws IOException {
        this.cityService.export(response, filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('money:city:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.cityService.imp(file);
    }


}

