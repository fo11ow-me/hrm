package com.qiujie.knowledge.controller;

import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.entity.KbUploadSession;
import com.qiujie.knowledge.mapper.KbUploadSessionMapper;
import com.qiujie.knowledge.service.KbUploadCompletionHandler;
import com.qiujie.service.FileUploadService;
import com.qiujie.spi.UploadSessionInfo;
import com.qiujie.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 知识库分片上传控制器。
 * 三阶段协议：init → chunk upload → complete
 */
@RestController
@RequestMapping("/knowledge/upload")
public class UploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private KbUploadCompletionHandler kbHandler;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private KbUploadSessionMapper sessionMapper;

    /**
     * 文件已通过统一的 /file-task/upload 上传，此处接收 uploadId 触发知识库文档摄入。
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO createFromUpload(@RequestParam String uploadId) {
        KbUploadSession session = sessionMapper.selectById(uploadId);
        if (session == null || session.getMergedObjectKey() == null) {
            return Response.error("上传会话不存在或文件未完成上传");
        }
        UploadSessionInfo info = new UploadSessionInfo(
                session.getUploadId(), session.getFileName(), session.getFileExt(),
                session.getFileSize(), session.getFileHash(),
                session.getStaffId(), session.getChunkCount());
        Map<String, Object> extra = kbHandler.onComplete(session.getMergedObjectKey(), info);
        return Response.success("文档已提交处理", extra);
    }

    @PostMapping("/init")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO initUpload(@RequestBody Map<String, Object> request) {
        String fileName = (String) request.get("fileName");
        String fileExt = (String) request.get("fileExt");
        Long fileSize = ((Number) request.get("fileSize")).longValue();
        String fileHash = (String) request.get("fileHash");
        Long chunkSize = request.get("chunkSize") != null
                ? ((Number) request.get("chunkSize")).longValue() : 5 * 1024 * 1024L;

        return fileUploadService.initUpload(fileName, fileExt, fileSize, fileHash, chunkSize,
                securityUtil.getCurrentOperatorId(), kbHandler);
    }

    @PostMapping("/chunks")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadChunk(uploadId, chunkIndex, file);
    }

    @PostMapping("/{uploadId}/complete")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO completeUpload(@PathVariable String uploadId) {
        return fileUploadService.completeUpload(uploadId, kbHandler);
    }
}
