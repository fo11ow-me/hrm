package com.qiujie.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    /** 将卡在 UPLOADED 或 PROCESSING 状态的僵死文档标记为 FAILED */
    @Update("UPDATE kb_document SET status = 'FAILED', failure_reason = #{reason}, "
          + "update_time = NOW() WHERE status IN ('UPLOADED', 'PROCESSING')")
    int markStaleProcessingAsFailed(String reason);
}
