package com.qiujie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.entity.FileTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface FileTaskMapper extends BaseMapper<FileTask> {

    @Update("update file_task set status = 'RUNNING', start_time = current_timestamp, update_time = current_timestamp " +
            "where id = #{id} and status = 'PENDING'")
    int claimRunning(@Param("id") Long id);

    @Update("update file_task set total_count = total_count + #{total}, processed_count = processed_count + #{processed}, " +
            "success_count = success_count + #{success}, fail_count = fail_count + #{fail}, update_time = current_timestamp " +
            "where id = #{id}")
    int increaseProgress(@Param("id") Long id, @Param("total") Integer total, @Param("processed") Integer processed,
                         @Param("success") Integer success, @Param("fail") Integer fail);
}
