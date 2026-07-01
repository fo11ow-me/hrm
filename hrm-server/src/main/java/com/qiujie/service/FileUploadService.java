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
 *
 * 协议流程：
 *   ① init   → 创建上传会话（秒传/断点续传/新建）
 *   ② chunks → 逐片上传分片到 MinIO（幂等，已传分片自动跳过）
 *   ③ complete → 合并分片为完整文件，回调业务 handler 执行后续逻辑
 *
 * 支持秒传（同 hash 文件已存在时跳过上传）和断点续传（未过期会话可续传未完成分片）。
 */
@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    /** 单文件最大 256MB */
    private static final long MAX_FILE_SIZE = 256 * 1024 * 1024L;
    /** 单分片最大 10MB */
    private static final long MAX_CHUNK_SIZE = 10 * 1024 * 1024L;
    /** 上传会话有效期 24 小时，超时后定时清理 */
    private static final int SESSION_TTL_HOURS = 24;

    @Autowired
    private KbUploadSessionMapper sessionMapper;

    @Autowired
    private KbUploadChunkMapper chunkMapper;

    @Autowired
    private MinioStorageService storageService;

    /**
     * 阶段1：初始化上传会话。
     *
     * 三级优先级决策：
     *   1. 秒传 — handler.checkDedup(fileHash) 命中 → 直接返回已有文件信息，跳过上传
     *   2. 断点续传 — 同 staffId + fileHash 存在 INIT/UPLOADING 状态会话 → 返回已有 uploadId 和已传分片列表
     *   3. 新建会话 — 写入 kb_upload_session，返回 uploadId、chunkSize、chunkCount
     *
     * @param fileName  原始文件名，如 "员工手册.pdf"
     * @param fileExt   文件扩展名（不含点），如 "pdf"
     * @param fileSize  文件总大小（字节）
     * @param fileHash  文件 SHA-256 哈希，用于秒传去重和断点续传匹配
     * @param chunkSize 单个分片大小（字节），前端默认 5MB
     * @param staffId   当前操作员工 ID
     * @param handler   业务回调处理器，知识库传入 KbUploadCompletionHandler，通用导入传入 null
     * @return uploadId + 可选 chunkSize/chunkCount/instantUpload/resumed/uploadedChunks
     */
    @Transactional
    public ResponseDTO initUpload(String fileName, String fileExt, Long fileSize,
                                   String fileHash, Long chunkSize,
                                   Integer staffId, UploadCompletionHandler handler) {
        // 文件大小校验
        if (fileSize > MAX_FILE_SIZE) {
            return Response.error("文件大小超过限制，最大256MB");
        }
        // 分片大小校验
        if (chunkSize > MAX_CHUNK_SIZE) {
            return Response.error("分片大小超过限制，最大10MB");
        }

        // —— 优先级 1: 秒传 ——
        // handler 为 null 时跳过（通用导入场景无需去重）
        if (handler != null) {
            Map<String, Object> dedup = handler.checkDedup(fileHash);
            if (dedup != null && !dedup.isEmpty()) {
                dedup.put("instantUpload", true);
                return Response.success("秒传成功，文件已存在", dedup);
            }
        }

        // —— 优先级 2: 断点续传 ——
        // 查找同一用户上传的相同 hash 且未完成/未过期的会话
        List<KbUploadSession> sessions = sessionMapper.selectList(
                new QueryWrapper<KbUploadSession>()
                        .eq("staff_id", staffId)
                        .eq("file_hash", fileHash)
                        .in("status", "INIT", "UPLOADING"));
        if (!sessions.isEmpty()) {
            KbUploadSession session = sessions.get(0);
            // 返回已上传的分片索引列表，前端据此跳过已传分片
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

        // —— 优先级 3: 新建会话 ——
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
                // 24 小时后过期，定时任务 cleanExpiredSessions() 清理
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
     * 阶段2：上传单个分片（幂等），校验分片哈希确保传输完整性。
     */
    @Transactional
    public ResponseDTO uploadChunk(String uploadId, Integer chunkIndex, String chunkHash, MultipartFile file) {
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
            String serverHash = SecureUtil.sha256(new ByteArrayInputStream(bytes));
            if (chunkHash != null && !chunkHash.isEmpty() && !serverHash.equalsIgnoreCase(chunkHash)) {
                log.warn("分片哈希不匹配: uploadId={}, chunkIndex={}, client={}, server={}",
                        uploadId, chunkIndex, chunkHash, serverHash);
                return Response.error("分片校验失败，请重试");
            }
            storageService.put(storagePath, bytes);
            KbUploadChunk chunk = new KbUploadChunk()
                    .setUploadId(uploadId)
                    .setChunkIndex(chunkIndex)
                    .setChunkSize((long) bytes.length)
                    .setChunkHash(serverHash)
                    .setStoragePath(storagePath);
            chunkMapper.insert(chunk);

            // 首个分片时更新状态
            if (!"UPLOADING".equals(session.getStatus())) {
                session.setStatus("UPLOADING");
                sessionMapper.updateById(session);
            }

            return Response.success(uploadId, Map.of("chunkIndex", chunkIndex));
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
