package com.qiujie.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.mapper.DocumentChunkMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库文档查询服务（只读薄层）：list/query/chunks。
 * 生命周期动词（摄入/重试/删除）归 {@code DocumentLifecycleService}。
 */
@Service
public class KnowledgeService {

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

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
     * 列出指定文档的所有切片内容
     */
    public ResponseDTO chunks(Long documentId) {
        List<com.qiujie.knowledge.entity.DocumentChunk> chunks =
                chunkMapper.selectByDocumentIdOrderByChunkIndex(documentId);
        return Response.success(chunks);
    }
}
