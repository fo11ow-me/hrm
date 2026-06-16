package com.qiujie.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.entity.KbUploadChunk;
import com.qiujie.knowledge.entity.KbUploadSession;
import com.qiujie.knowledge.mapper.KbUploadChunkMapper;
import com.qiujie.knowledge.mapper.KbUploadSessionMapper;
import com.qiujie.spi.UploadCompletionHandler;
import com.qiujie.spi.UploadSessionInfo;
import com.qiujie.storage.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 三阶段分片上传服务。
 * init → chunk upload → complete，支持秒传和断点续传。
 */
@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    private static final long MAX_FILE_SIZE = 256 * 1024 * 1024L; // 256MB
    private static final long MAX_CHUNK_SIZE = 10 * 1024 * 1024L;   // 10MB
    private static final int SESSION_TTL_HOURS = 24;

    @Autowired
    private KbUploadSessionMapper sessionMapper;

    @Autowired
    private KbUploadChunkMapper chunkMapper;

    @Autowired
    private MinioStorageService storageService;

    /**
     * 阶段1：初始化上传会话。
     * 优先级：秒传 → 断点续传(未过期会话) → 新建会话
     */
    @Transactional
    public ResponseDTO initUpload(String fileName, String fileExt, Long fileSize,
                                   String fileHash, Long chunkSize,
                                   Integer staffId, UploadCompletionHandler handler) {
        if (fileSize > MAX_FILE_SIZE) {
            return Response.error("文件大小超过限制，最大256MB");
        }
        if (chunkSize > MAX_CHUNK_SIZE) {
            return Response.error("分片大小超过限制，最大10MB");
        }

        // 秒传检测（无 handler 的场景如导入任务跳过）
        if (handler != null) {
            Map<String, Object> dedup = handler.checkDedup(fileHash);
            if (dedup != null && !dedup.isEmpty()) {
                dedup.put("instantUpload", true);
                return Response.success("秒传成功，文件已存在", dedup);
            }
        }

        // 断点续传：查找未过期的会话（INIT 或 UPLOADING 状态均可续传）
        List<KbUploadSession> sessions = sessionMapper.selectList(
                new QueryWrapper<KbUploadSession>()
                        .eq("staff_id", staffId)
                        .eq("file_hash", fileHash)
                        .in("status", "INIT", "UPLOADING"));
        if (!sessions.isEmpty()) {
            KbUploadSession session = sessions.get(0);
            List<Integer> uploadedChunks = chunkMapper.selectList(
                    new QueryWrapper<KbUploadChunk>().eq("upload_id", session.getUploadId()))
                    .stream().map(KbUploadChunk::getChunkIndex).collect(Collectors.toList());
            Map<String, Object> result = new HashMap<>();
            result.put("instantUpload", false);
            result.put("resumed", true);
            result.put("uploadId", session.getUploadId());
            result.put("uploadedChunks", uploadedChunks);
            return Response.success("恢复上传会话", result);
        }

        // 新建会话
        int chunkCount = (int) Math.ceil((double) fileSize / chunkSize);
        String uploadId = IdUtil.fastSimpleUUID();
        KbUploadSession session = new KbUploadSession()
                .setUploadId(uploadId)
                .setStaffId(staffId)
                .setFileName(fileName)
                .setFileExt(fileExt)
                .setFileSize(fileSize)
                .setFileHash(fileHash)
                .setChunkSize(chunkSize)
                .setChunkCount(chunkCount)
                .setStatus("INIT")
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_TTL_HOURS));
        sessionMapper.insert(session);

        Map<String, Object> result = new HashMap<>();
        result.put("instantUpload", false);
        result.put("resumed", false);
        result.put("uploadId", uploadId);
        result.put("chunkSize", chunkSize);
        result.put("chunkCount", chunkCount);
        return Response.success(result);
    }

    /**
     * 阶段2：上传单个分片（幂等）。
     */
    @Transactional
    public ResponseDTO uploadChunk(String uploadId, Integer chunkIndex, MultipartFile file) {
        KbUploadSession session = sessionMapper.selectById(uploadId);
        if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Response.error("上传会话不存在或已过期");
        }

        // 幂等：不重复上传已有分片
        if (chunkMapper.selectCount(new QueryWrapper<KbUploadChunk>()
                .eq("upload_id", uploadId).eq("chunk_index", chunkIndex)) > 0) {
            return Response.success("分片已存在");
        }

        String storagePath = String.format("uploads/%s/chunks/%d", uploadId, chunkIndex);
        try {
            byte[] bytes = file.getBytes();
            storageService.put(storagePath, bytes);
            KbUploadChunk chunk = new KbUploadChunk()
                    .setUploadId(uploadId)
                    .setChunkIndex(chunkIndex)
                    .setChunkSize((long) bytes.length)
                    .setChunkHash(SecureUtil.sha256(new ByteArrayInputStream(bytes)))
                    .setStoragePath(storagePath);
            chunkMapper.insert(chunk);

            // 首个分片时更新状态
            if (!"UPLOADING".equals(session.getStatus())) {
                session.setStatus("UPLOADING");
                sessionMapper.updateById(session);
            }

            List<Integer> uploaded = chunkMapper.selectList(
                    new QueryWrapper<KbUploadChunk>().eq("upload_id", uploadId))
                    .stream().map(KbUploadChunk::getChunkIndex).collect(Collectors.toList());
            return Response.success(uploaded);
        } catch (IOException e) {
            log.error("分片上传失败: uploadId={}, chunkIndex={}", uploadId, chunkIndex, e);
            return Response.error("分片上传失败");
        }
    }

    /**
     * 阶段3（无 handler）：仅合并分片，返回 MinIO key。
     * 供导入任务等自行创建 FileTask 的场景使用。
     */
    @Transactional
    public String completeUpload(String uploadId) {
        KbUploadSession session = sessionMapper.selectById(uploadId);
        if (session == null) {
            throw new RuntimeException("上传会话不存在");
        }
        // 直接上传已完成，无需合并分片
        if ("COMPLETED".equals(session.getStatus()) && session.getMergedObjectKey() != null) {
            return session.getMergedObjectKey();
        }
        List<KbUploadChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<KbUploadChunk>().eq("upload_id", uploadId).orderByAsc("chunk_index"));
        if (chunks.size() != session.getChunkCount()) {
            throw new RuntimeException("分片不完整，已上传 " + chunks.size() + "/" + session.getChunkCount());
        }
        session.setStatus("COMPLETING");
        sessionMapper.updateById(session);

        String mergedKey = String.format("task-source/%d/%s/%s.%s",
                session.getStaffId(), uploadId,
                IdUtil.fastSimpleUUID().substring(2, 22),
                session.getFileExt());
        try {
            List<String> sourceKeys = chunks.stream()
                    .map(KbUploadChunk::getStoragePath).collect(Collectors.toList());
            storageService.composeObject(sourceKeys, mergedKey, "application/octet-stream");
            session.setMergedObjectKey(mergedKey);
            session.setStatus("COMPLETED");
            sessionMapper.updateById(session);
            return mergedKey;
        } catch (Exception e) {
            log.error("合并分片失败: uploadId={}", uploadId, e);
            try { storageService.delete(mergedKey); } catch (Exception ignored) {}
            throw new RuntimeException("合并分片失败", e);
        }
    }

    /**
     * 阶段3：完成上传，合并分片并通过 handler 执行业务逻辑。
     */
    @Transactional
    public ResponseDTO completeUpload(String uploadId, UploadCompletionHandler handler) {
        KbUploadSession session = sessionMapper.selectById(uploadId);
        if (session == null) {
            return Response.error("上传会话不存在");
        }

        List<KbUploadChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<KbUploadChunk>().eq("upload_id", uploadId).orderByAsc("chunk_index"));
        if (chunks.size() != session.getChunkCount()) {
            return Response.error("分片不完整，已上传 " + chunks.size() + "/" + session.getChunkCount());
        }

        session.setStatus("COMPLETING");
        sessionMapper.updateById(session);

        String mergedKey = String.format("%s/%d/%s/%s.%s",
                handler.getStoragePrefix(), session.getStaffId(), uploadId,
                IdUtil.fastSimpleUUID().substring(2, 22),
                session.getFileExt());
        try {
            List<String> sourceKeys = chunks.stream()
                    .map(KbUploadChunk::getStoragePath).collect(Collectors.toList());
            storageService.composeObject(sourceKeys, mergedKey, "application/octet-stream");

            UploadSessionInfo info = new UploadSessionInfo(
                    uploadId, session.getFileName(), session.getFileExt(),
                    session.getFileSize(), session.getFileHash(),
                    session.getStaffId(), session.getChunkCount());
            Map<String, Object> extra = handler.onComplete(mergedKey, info);

            session.setMergedObjectKey(mergedKey);
            session.setStatus("COMPLETED");
            sessionMapper.updateById(session);

            if (extra == null) extra = new HashMap<>();
            return Response.success("上传完成", extra);
        } catch (Exception e) {
            log.error("合并分片失败: uploadId={}", uploadId, e);
            try { storageService.delete(mergedKey); } catch (Exception ignored) {}
            return Response.error("合并分片失败");
        }
    }

    /**
     * 小文件直接上传（无需分片），存入 MinIO 并返回 uploadId 语义的 key。
     */
    public ResponseDTO uploadDirect(MultipartFile file, Integer staffId) {
        if (file.getSize() > MAX_CHUNK_SIZE) {
            return Response.error("文件超过10MB，请使用分片上传");
        }
        String uploadId = IdUtil.fastSimpleUUID();
        String ext = file.getOriginalFilename() != null
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1)
                : "bin";
        String key = String.format("task-source/%d/%s/%s.%s",
                staffId != null ? staffId : -1, uploadId, IdUtil.fastSimpleUUID().substring(2, 22), ext);
        try {
            storageService.put(key, file.getBytes());
            // 插入一条虚拟 session 记录，口径统一：/import/task 通过 uploadId 查询
            KbUploadSession session = new KbUploadSession()
                    .setUploadId(uploadId)
                    .setStaffId(staffId != null ? staffId : -1)
                    .setFileName(file.getOriginalFilename())
                    .setFileExt(ext)
                    .setFileSize(file.getSize())
                    .setFileHash("DIRECT")
                    .setChunkSize(file.getSize())
                    .setChunkCount(1)
                    .setMergedObjectKey(key)
                    .setStatus("COMPLETED")
                    .setExpiresAt(LocalDateTime.now().plusHours(SESSION_TTL_HOURS));
            sessionMapper.insert(session);
            Map<String, Object> result = new HashMap<>();
            result.put("uploadId", uploadId);
            result.put("mergedKey", key);
            return Response.success(result);
        } catch (IOException e) {
            log.error("Direct upload failed: {}", file.getOriginalFilename(), e);
            return Response.error("上传失败");
        }
    }

    /**
     * 每小时清理过期上传会话及其分片文件。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpiredSessions() {
        List<KbUploadSession> expired = sessionMapper.selectList(
                new QueryWrapper<KbUploadSession>().lt("expires_at", LocalDateTime.now()));
        for (KbUploadSession session : expired) {
            // 清理分片文件
            List<KbUploadChunk> chunks = chunkMapper.selectList(
                    new QueryWrapper<KbUploadChunk>().eq("upload_id", session.getUploadId()));
            for (KbUploadChunk chunk : chunks) {
                try { storageService.delete(chunk.getStoragePath()); } catch (Exception ignored) {}
            }
            chunkMapper.delete(new QueryWrapper<KbUploadChunk>().eq("upload_id", session.getUploadId()));
            sessionMapper.deleteById(session.getUploadId());
        }
        if (!expired.isEmpty()) {
            log.info("Cleaned {} expired upload sessions", expired.size());
        }
    }
}
