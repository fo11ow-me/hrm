package com.qiujie.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.qiujie.spi.UploadCompletionHandler;
import com.qiujie.spi.UploadSessionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库上传完成处理器：分片上传协议与文档生命周期的薄适配器。
 * 业务逻辑（建文档、事务提交后异步 ETL、状态机）全部在 {@link DocumentLifecycleService} 内部。
 */
@Component
public class KbUploadCompletionHandler implements UploadCompletionHandler {

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private DocumentLifecycleService lifecycle;

    @Override
    public String getStoragePrefix() {
        return "knowledge";
    }

    @Override
    public Map<String, Object> checkDedup(String fileHash) {
        List<KnowledgeDocument> existing = documentMapper.selectList(
                new QueryWrapper<KnowledgeDocument>().eq("file_hash", fileHash));
        if (existing.isEmpty()) return null;
        KnowledgeDocument doc = existing.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("documentId", doc.getId());
        result.put("name", doc.getName());
        return result;
    }

    @Override
    public Map<String, Object> onComplete(String mergedKey, UploadSessionInfo session) {
        DocumentLifecycleService.RegisterResult r = lifecycle.register(
                new DocumentLifecycleService.RegisterCommand(
                        mergedKey, session.getFileName(), session.getFileExt(),
                        session.getFileHash(), session.getFileSize(), session.getStaffId()));
        Map<String, Object> result = new HashMap<>();
        result.put("documentId", r.documentId());
        result.put("status", r.status());
        return result;
    }
}
