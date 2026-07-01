package com.qiujie.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.event.DocumentIngestionEvent;
import com.qiujie.knowledge.mapper.DocumentChunkMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import com.qiujie.storage.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库文档管理服务。
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public ResponseDTO list(int current, int size, String oldName) {
        com.baomidou.mybatisplus.core.metadata.IPage<KnowledgeDocument> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
        QueryWrapper<KnowledgeDocument> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        if (oldName != null && !oldName.isBlank()) {
            wrapper.like("old_name", oldName);
        }
        var result = documentMapper.selectPage(page, wrapper);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("pages", result.getPages());
        map.put("total", result.getTotal());
        map.put("list", result.getRecords());
        return Response.success(map);
    }

    public ResponseDTO query(Long id) {
        KnowledgeDocument doc = documentMapper.selectById(id);
        if (doc != null) {
            return Response.success(doc);
        }
        return Response.error("文档不存在");
    }

    /**
     * 删除文档：级联清理物理文件 + 切片 + 向量
     */
    @Transactional
    public ResponseDTO delete(Long id) {
        KnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return Response.error("文档不存在");
        }

        // 检查是否有其他文档引用同一物理文件
        long refCount = documentMapper.selectCount(
                new QueryWrapper<KnowledgeDocument>().eq("name", doc.getName()).ne("id", id));
        if (refCount == 0) {
            try { storageService.delete(doc.getName()); } catch (Exception e) {
                log.warn("删除物理文件失败: {}", doc.getName(), e);
            }
        }

        // 删除切片记录
        chunkMapper.deleteByDocumentId(id);

        // 逻辑删除元数据
        documentMapper.deleteById(id);
        return Response.success();
    }

    /**
     * 重试失败的文档摄入
     */
    @Transactional
    public ResponseDTO retry(Long id) {
        KnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return Response.error("文档不存在");
        }
        if (!"FAILED".equals(doc.getStatus())) {
            return Response.error("仅失败状态的文档可重试");
        }

        doc.setStatus("PROCESSING");
        doc.setFailureReason(null);
        documentMapper.updateById(doc);

        eventPublisher.publishEvent(new DocumentIngestionEvent(doc.getId()));
        return Response.success("已重新提交处理");
    }

    /**
     * 列出指定文档的所有切片内容
     */
    public ResponseDTO chunks(Long documentId) {
        List<com.qiujie.knowledge.entity.DocumentChunk> chunks =
                chunkMapper.selectByDocumentIdOrderByChunkIndex(documentId);
        return Response.success(chunks);
    }
}
