package com.qiujie.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.enums.DocumentStatusEnum;
import com.qiujie.knowledge.event.DocumentIngestionEvent;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.qiujie.spi.UploadCompletionHandler;
import com.qiujie.spi.UploadSessionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库上传完成处理器：创建 KnowledgeDocument + 发布 ETL 事件。
 */
@Component
public class KbUploadCompletionHandler implements UploadCompletionHandler {

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public String getStoragePrefix() {
        return "knowledge";
    }

    @Override
    public Map<String, Object> checkDedup(String fileHash) {
        List<KnowledgeDocument> existing = documentMapper.selectList(
                new QueryWrapper<KnowledgeDocument>()
                        .eq("file_hash", fileHash)
                        .eq("status", "READY")
                        .eq("is_deleted", 0));
        if (existing.isEmpty()) return null;
        KnowledgeDocument doc = existing.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("documentId", doc.getId());
        result.put("name", doc.getName());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> onComplete(String mergedKey, UploadSessionInfo session) {
        KnowledgeDocument doc = new KnowledgeDocument()
                .setName(mergedKey)
                .setOldName(session.getFileName())
                .setType(session.getFileExt())
                .setFileHash(session.getFileHash())
                .setFileSize(session.getFileSize())
                .setStatus(DocumentStatusEnum.PROCESSING.name())
                .setStaffId(session.getStaffId());
        documentMapper.insert(doc);

        eventPublisher.publishEvent(new DocumentIngestionEvent(doc.getId()));

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", doc.getId());
        result.put("status", doc.getStatus());
        return result;
    }
}
