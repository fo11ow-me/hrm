package com.qiujie.service;

import com.qiujie.entity.FileTask;
import com.qiujie.enums.TaskStatusEnum;

import java.io.File;

/**
 * 文件任务执行所需的运行时端口。
 * <p>执行引擎只依赖任务状态、错误记录和文件副作用，不依赖具体持久化或存储实现。</p>
 */
public interface FileTaskRuntimePort {

    boolean claimRunning(Long taskId);

    FileTask getById(Long taskId);

    void setTotalCount(Long taskId, int total);

    void increaseProgress(Long taskId, int total, int processed, int success, int fail);

    void finish(Long taskId, TaskStatusEnum status);

    void fail(Long taskId, Exception exception);

    void deleteSourceFile(Long taskId);

    void generateErrorFile(Long taskId);

    File buildTaskFile(String subDir, String originalFilename);

    String uploadToMinio(File file, String subDir);

    void setResultFile(Long taskId, String resultFilePath);
}
