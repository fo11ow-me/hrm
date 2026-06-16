package com.qiujie.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    @Update("UPDATE kb_document SET status = 'FAILED', failure_reason = #{reason}, updated_at = NOW() WHERE status = 'PROCESSING' AND is_deleted = 0")
    int markStaleProcessingAsFailed(String reason);
}
