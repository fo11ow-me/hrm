package com.qiujie.filetask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 任务文件介质端口——统一本地临时路径与 MinIO 对象存储。
 * <p>
 * 业务与执行引擎不再判断路径格式，本地历史路径与对象存储 key 的差异由实现适配。
 * </p>
 */
public interface ArtifactStore {

    /** 是否为远程对象存储 key（而非本地路径）；null 视为本地。 */
    boolean isRemote(String path);

    /** 解析源文件：远程 key 下载到临时文件，本地路径直接返回文件引用。 */
    File resolveSource(String source);

    /** 在临时目录创建任务文件（UUID 文件名，保留原扩展名）。 */
    File createTaskFile(String subDir, String originalFilename);

    /** 上传文件至对象存储并删除本地临时文件，返回存储 key。 */
    String upload(File file, String subDir);

    /** 删除本地路径或远程对象；null/空串忽略。 */
    void delete(String path);

    /** 本地路径或远程对象是否存在。 */
    boolean exists(String path);

    /** 打开本地路径或远程对象的输入流。 */
    InputStream open(String path) throws IOException;
}