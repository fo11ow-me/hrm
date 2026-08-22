package com.qiujie.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.knowledge.entity.IngestionJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文档摄入异步任务 Mapper
 *
 * @author quuj
 */
@Mapper
public interface IngestionJobMapper extends BaseMapper<IngestionJob> {

    /** 作废文档的在途作业（删除文档时防 worker 继续写 PG）。 */
    @Update("UPDATE ingestion_jobs SET status = 'CANCELLED', finished_at = NOW() "
          + "WHERE document_id = #{documentId} AND status IN ('PENDING', 'RUNNING')")
    int cancelActiveJobs(@Param("documentId") Long documentId);

    /** 启动回收：遗留 RUNNING 作业 → FAILED。 */
    @Update("UPDATE ingestion_jobs SET status = 'FAILED', last_error = #{error}, finished_at = NOW() "
          + "WHERE status = 'RUNNING'")
    int markStaleRunningAsFailed(@Param("error") String error);
}
