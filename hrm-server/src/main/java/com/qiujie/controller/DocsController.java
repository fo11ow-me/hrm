package com.qiujie.controller;

import com.qiujie.entity.Docs;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.DocsService;
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
 * 文件上传接口
 *
 * @Author qiujie
 * @Date 2022/2/24
 * @Version 1.0
 */

@RestController
@RequestMapping("/docs")
public class DocsController {

    @Autowired
    private DocsService docsService;

    @ApiOperation("新增")
    @PostMapping
    public ResponseDTO add(@RequestBody Docs docs) {
        return this.docsService.add(docs);
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:docs:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.docsService.delete(id);
    }

    @ApiOperation("批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('system:docs:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.docsService.deleteBatch(ids);
    }

    @ApiOperation("编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:docs:edit')")
    public ResponseDTO edit(@RequestBody Docs docs) {
        return this.docsService.edit(docs);
    }

    @ApiOperation("查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.docsService.query(id);
    }

    @ApiOperation("分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:docs:list','system:docs:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String oldName, String staffName) {
        return this.docsService.list(current, size, oldName, staffName);
    }

    @ApiOperation("数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('system:docs:export')")
    public void export(HttpServletResponse response,@PathVariable  String filename) throws IOException {
        this.docsService.export(response,filename);
    }

    @ApiOperation("数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('system:docs:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.docsService.imp(file);
    }


    @ApiOperation("文件上传")
    @PostMapping("/upload/{id}")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO upload(MultipartFile file, @PathVariable Integer id) throws IOException {
        return this.docsService.upload(file, id);
    }

    @ApiOperation("文件下载")
    @GetMapping("/download/{filename}")
    @PreAuthorize("hasAnyAuthority('system:docs:download')")
    public void download(@PathVariable String filename, HttpServletResponse response) throws IOException {
        this.docsService.download(filename, response);
    }

    @ApiOperation("文件下载")
    @GetMapping("/avatar/{filename}")
    public void getAvatar(@PathVariable String filename, HttpServletResponse response) throws IOException {
        this.docsService.download(filename, response);
    }
}
