package com.qiujie.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.knowledge.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    /** 启动恢复：执行中崩溃的 PROCESSING 文档 → FAILED（UPLOADED 由恢复器续跑，不在此处理）。 */
    @Update("UPDATE kb_document SET status = 'FAILED', failure_reason = #{reason}, "
          + "update_time = NOW() WHERE status = 'PROCESSING' AND is_deleted = 0")
    int markStaleProcessingAsFailed(String reason);

    /** 启动恢复：未启动的 UPLOADED 文档（供续跑 ETL）。 */
    @Select("SELECT * FROM kb_document WHERE status = 'UPLOADED' AND is_deleted = 0")
    java.util.List<KnowledgeDocument> selectLiveUploaded();

    /** 启动恢复：已逻辑删除的文档（供重跑物理清理）。 */
    @Select("SELECT * FROM kb_document WHERE is_deleted = 1")
    java.util.List<KnowledgeDocument> selectDeleted();

    /**
     * CAS 认领：仅当状态为 UPLOADED/FAILED 且未删除时置 PROCESSING。
     * 唯一的并发互斥原语——PROCESSING ⟺ 恰好一个管道拥有该文档。
     */
    @Update("UPDATE kb_document SET status = 'PROCESSING', update_time = NOW() "
          + "WHERE id = #{id} AND status IN ('UPLOADED', 'FAILED') AND is_deleted = 0")
    int claimForProcessing(Long id);

    /** CAS 结算：PROCESSING → READY，一并写 chunk_count/preview_text/process_time 并清除失败原因；已删除则失败（防复活）。 */
    @Update("UPDATE kb_document SET status = 'READY', preview_text = #{previewText}, "
          + "chunk_count = #{chunkCount}, failure_reason = NULL, process_time = NOW(), update_time = NOW() "
          + "WHERE id = #{id} AND status = 'PROCESSING' AND is_deleted = 0")
    int completeProcessing(@Param("id") Long id, @Param("previewText") String previewText,
                           @Param("chunkCount") int chunkCount);

    /** 状态机补偿：PROCESSING → FAILED + failure_reason（已删除则跳过）。 */
    @Update("UPDATE kb_document SET status = 'FAILED', failure_reason = #{reason}, update_time = NOW() "
          + "WHERE id = #{id} AND status = 'PROCESSING' AND is_deleted = 0")
    int markFailed(@Param("id") Long id, @Param("reason") String reason);

    /** 存活文档引用计数：同物理文件（name）且未删除、排除自身。 */
    @Select("SELECT COUNT(*) FROM kb_document WHERE name = #{name} AND is_deleted = 0 AND id != #{excludeId}")
    long countLiveByFileName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /** CAS 逻辑删除（幂等：已删除时影响行数为 0）。 */
    @Update("UPDATE kb_document SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int markDeleted(Long id);
}
