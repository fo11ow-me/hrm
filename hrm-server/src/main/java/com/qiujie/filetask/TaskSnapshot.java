package com.qiujie.filetask;

import com.qiujie.entity.FileTask;

/** 异步任务提交后的初始快照。 */
public record TaskSnapshot(Long taskId, FileTask snapshot) {
}
