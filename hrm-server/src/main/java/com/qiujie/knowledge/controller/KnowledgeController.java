package com.qiujie.knowledge.controller;

import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.dto.QaRequest;
import com.qiujie.knowledge.entity.KbUploadSession;
import com.qiujie.knowledge.mapper.KbUploadSessionMapper;
import com.qiujie.knowledge.service.KbUploadCompletionHandler;
import com.qiujie.knowledge.service.KnowledgeService;
import com.qiujie.knowledge.service.QaService;
import com.qiujie.service.FileUploadService;
import com.qiujie.spi.UploadSessionInfo;
import com.qiujie.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 知识库控制器：文档管理 + 分片上传 + RAG 问答。
 *
 * @author quuj
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private QaService qaService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private KbUploadCompletionHandler kbHandler;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private KbUploadSessionMapper sessionMapper;

    // ==================== 文档管理 ====================

    /**
     * 分页查询知识库文档列表，支持按原始文件名筛选。
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:docs:list')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) String oldName) {
        return knowledgeService.list(current, size, oldName);
    }

    /**
     * 查询单个知识库文档详情。
     */
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Long id) {
        return knowledgeService.query(id);
    }

    /**
     * 逻辑删除知识库文档，同时移除 MinIO 文件和切片数据。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:docs:delete')")
    public ResponseDTO delete(@PathVariable Long id) {
        return knowledgeService.delete(id);
    }

    /**
     * 重试失败的文档摄入任务。
     */
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO retry(@PathVariable Long id) {
        return knowledgeService.retry(id);
    }

    /**
     * 查看文档的切片列表，用于调试和验证 ETL 效果。
     */
    @GetMapping("/{id}/chunks")
    public ResponseDTO chunks(@PathVariable Long id) {
        return knowledgeService.chunks(id);
    }

    // ==================== 分片上传 ====================

    /**
     * 分片上传初始化，创建上传会话并返回 uploadId。
     */
    @PostMapping("/upload/init")
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

    /**
     * 上传单个分片，将分片数据写入 MinIO 临时存储。
     */
    @PostMapping("/upload/chunks")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam(value = "chunkHash", required = false) String chunkHash,
            @RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadChunk(uploadId, chunkIndex, chunkHash, file);
    }

    /**
     * 合并所有分片为完整文件，回调知识库 Handler 创建文档记录并触发 ETL 摄入。
     */
    @PostMapping("/upload/{uploadId}/complete")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO completeUpload(@PathVariable String uploadId) {
        return fileUploadService.completeUpload(uploadId, kbHandler);
    }

    /**
     * 从已上传文件创建知识库文档，适用于上传完成后再补发摄入的场景。
     */
    @PostMapping("/upload")
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

    // ==================== RAG 问答 ====================

    /**
     * 同步 RAG 问答：检索知识库 → LLM 生成回答 → 返回引用来源。
     */
    @PostMapping("/qa/ask")
    public ResponseDTO ask(@RequestBody QaRequest request) {
        return qaService.ask(request);
    }

    /**
     * SSE 流式 RAG 问答，逐 token 推送生成结果和引用来源。
     */
    @PostMapping(value = "/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@RequestBody QaRequest request) {
        return qaService.streamAsk(request);
    }
}
