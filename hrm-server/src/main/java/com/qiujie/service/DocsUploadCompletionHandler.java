package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.entity.Docs;
import com.qiujie.spi.UploadCompletionHandler;
import com.qiujie.spi.UploadSessionInfo;
import com.qiujie.mapper.DocsMapper;
import com.qiujie.storage.MinioStorageService;
import com.qiujie.util.StorageCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用文档上传完成处理器：SHA256 秒传 + zstd 压缩 + 写入 sys_docs。
 */
@Component
public class DocsUploadCompletionHandler implements UploadCompletionHandler {

    private static final Logger log = LoggerFactory.getLogger(DocsUploadCompletionHandler.class);

    @Autowired
    private DocsMapper docsMapper;

    @Autowired
    private MinioStorageService storageService;

    @Override
    public String getStoragePrefix() {
        return "docs";
    }

    @Override
    public Map<String, Object> checkDedup(String fileHash) {
        List<Docs> existing = docsMapper.selectList(
                new QueryWrapper<Docs>().eq("file_hash", fileHash));
        if (existing.isEmpty()) return null;
        Docs doc = existing.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("id", doc.getId());
        result.put("name", doc.getName());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> onComplete(String mergedKey, UploadSessionInfo session) {
        // 读取合并后的文件内容用于压缩
        byte[] rawBytes;
        try (java.io.InputStream in = storageService.get(mergedKey)) {
            rawBytes = in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("读取合并文件失败: " + mergedKey, e);
        }

        // zstd 压缩
        StorageCompressor.CompressionResult compressed =
                StorageCompressor.tryCompress(rawBytes, session.getFileExt().toLowerCase());
        if (compressed.compressed) {
            log.info("文件压缩存储: {} ({} -> {} bytes)", mergedKey, rawBytes.length, compressed.bytes.length);
            storageService.put(mergedKey, compressed.bytes);
        }

        // 写入 sys_docs
        Docs docs = new Docs()
                .setName(mergedKey)
                .setOldName(session.getFileName())
                .setType(session.getFileExt())
                .setFileHash(session.getFileHash())
                .setSize((long) rawBytes.length / 1024)
                .setStoredSize((long) compressed.bytes.length)
                .setCompressed(compressed.compressed ? 1 : 0)
                .setStaffId(session.getStaffId());
        docsMapper.insert(docs);

        Map<String, Object> result = new HashMap<>();
        result.put("id", docs.getId());
        result.put("name", docs.getName());
        result.put("compressed", compressed.compressed);
        return result;
    }
}
