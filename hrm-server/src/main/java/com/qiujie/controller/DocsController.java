package com.qiujie.controller;

import com.qiujie.entity.Docs;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.FileUploadService;
import com.qiujie.service.DocsService;
import com.qiujie.service.DocsUploadCompletionHandler;
import com.qiujie.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private DocsUploadCompletionHandler docsHandler;

    @Autowired
    private SecurityUtil securityUtil;

    @Operation(summary = "新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO add(@RequestBody Docs docs) {
        return this.docsService.add(docs);
    }

    @Operation(summary = "逻辑删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:docs:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.docsService.delete(id);
    }

    @Operation(summary = "批量逻辑删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('system:docs:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.docsService.deleteBatch(ids);
    }

    @Operation(summary = "编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:docs:edit')")
    public ResponseDTO edit(@RequestBody Docs docs) {
        return this.docsService.edit(docs);
    }

    @Operation(summary = "查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.docsService.query(id);
    }

    @Operation(summary = "分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:docs:list','system:docs:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size, String oldName, String staffName) {
        return this.docsService.list(current, size, oldName, staffName);
    }

    @Operation(summary = "数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('system:docs:export')")
    public void export(HttpServletResponse response,@PathVariable  String filename) throws IOException {
        this.docsService.export(response,filename);
    }

    @Operation(summary = "数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('system:docs:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.docsService.imp(file);
    }


    @Operation(summary = "文件下载")
    @GetMapping("/download/{filename}")
    @PreAuthorize("hasAnyAuthority('system:docs:download')")
    public void download(@PathVariable String filename, HttpServletResponse response) throws IOException {
        this.docsService.download(filename, response);
    }

    @Operation(summary = "头像下载")
    @GetMapping("/avatar/{filename}")
    public void getAvatar(@PathVariable String filename, HttpServletResponse response) throws IOException {
        this.docsService.download(filename, response);
    }

    // ========== 分片上传（三阶段协议）==========

    @Operation(summary = "头像上传（小文件直传）")
    @PostMapping("/upload/{id}")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO upload(MultipartFile file, @PathVariable Integer id) throws IOException {
        return this.docsService.upload(file, id);
    }

    @Operation(summary = "分片上传-初始化")
    @PostMapping("/upload/init")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO initChunkedUpload(@RequestBody Map<String, Object> request) {
        String fileName = (String) request.get("fileName");
        String fileExt = (String) request.get("fileExt");
        Long fileSize = ((Number) request.get("fileSize")).longValue();
        String fileHash = (String) request.get("fileHash");
        Long chunkSize = request.get("chunkSize") != null
                ? ((Number) request.get("chunkSize")).longValue() : 5 * 1024 * 1024L;

        return fileUploadService.initUpload(fileName, fileExt, fileSize, fileHash, chunkSize,
                securityUtil.getCurrentOperatorId(), docsHandler);
    }

    @Operation(summary = "分片上传-上传分片")
    @PostMapping("/upload/chunks")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadChunk(uploadId, chunkIndex, file);
    }

    @Operation(summary = "分片上传-完成")
    @PostMapping("/upload/{uploadId}/complete")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO completeChunkedUpload(@PathVariable String uploadId) {
        return fileUploadService.completeUpload(uploadId, docsHandler);
    }
}
