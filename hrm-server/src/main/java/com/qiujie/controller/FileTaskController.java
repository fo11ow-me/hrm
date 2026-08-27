package com.qiujie.controller;

import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.FileTaskCoordinator;
import com.qiujie.service.FileUploadService;
import com.qiujie.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/file-task")
public class FileTaskController {

    @Autowired
    private FileTaskCoordinator fileTaskCoordinator;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private SecurityUtil securityUtil;

    @Operation(summary = "SSE 订阅任务状态更新")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return this.fileTaskCoordinator.subscribe();
    }

    @Operation(summary = "查询文件任务")
    @GetMapping
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String taskType,
                            String module) {
        return this.fileTaskCoordinator.list(current, size, taskType, module);
    }

    @Operation(summary = "查询文件任务详情")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Long id) {
        return this.fileTaskCoordinator.inspect(id);
    }

    @Operation(summary = "查询导入错误明细")
    @GetMapping("/{id}/errors")
    public ResponseDTO queryErrors(@PathVariable Long id,
                                   @RequestParam(defaultValue = "1") Integer current,
                                   @RequestParam(defaultValue = "10") Integer size) {
        return this.fileTaskCoordinator.queryErrors(id, current, size);
    }

    @Operation(summary = "下载任务文件")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, @RequestParam(defaultValue = "RESULT") String fileType,
                         HttpServletResponse response) throws IOException {
        this.fileTaskCoordinator.download(id, fileType, response);
    }

    // ========== 分片上传（三阶段协议）==========

    @Operation(summary = "分片上传-初始化")
    @PostMapping("/upload/init")
    @PreAuthorize("hasAnyAuthority('system:docs:upload','performance:attendance:import','performance:overtime:import','performance:leave:import','money:salary:import')")
    public ResponseDTO initUpload(@RequestBody Map<String, Object> request) {
        String fileName = (String) request.get("fileName");
        String fileExt = (String) request.get("fileExt");
        Long fileSize = ((Number) request.get("fileSize")).longValue();
        String fileHash = (String) request.get("fileHash");
        Long chunkSize = request.get("chunkSize") != null
                ? ((Number) request.get("chunkSize")).longValue() : 5 * 1024 * 1024L;

        return fileUploadService.initUpload(fileName, fileExt, fileSize, fileHash, chunkSize,
                securityUtil.getCurrentOperatorId(), null);
    }

    @Operation(summary = "分片上传-上传分片")
    @PreAuthorize("hasAnyAuthority('system:docs:upload','performance:attendance:import','performance:overtime:import','performance:leave:import','money:salary:import')")
    @PostMapping("/upload/chunks")
    public ResponseDTO uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam(value = "chunkHash", required = false) String chunkHash,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return fileUploadService.uploadChunk(uploadId, chunkIndex, chunkHash, file);
    }

    @PreAuthorize("hasAnyAuthority('system:docs:upload','performance:attendance:import','performance:overtime:import','performance:leave:import','money:salary:import')")
    @Operation(summary = "分片上传-完成（不含业务处理，仅合并返回 key）")
    @PostMapping("/upload/{uploadId}/complete")
    public ResponseDTO completeUpload(@PathVariable String uploadId) {
        String mergedKey = fileUploadService.completeUpload(uploadId);
        Map<String, Object> result = new HashMap<>();
        result.put("mergedKey", mergedKey);
        return Response.success(result);
    }
}
