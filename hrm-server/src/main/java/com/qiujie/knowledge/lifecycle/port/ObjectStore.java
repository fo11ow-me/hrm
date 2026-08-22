package com.qiujie.knowledge.lifecycle.port;

import java.io.InputStream;

/**
 * 对象存储端口（MinIO）。生命周期只需要两个动词：读原始文件、删物理文件。
 * put/composeObject 属上传模块职责，不进本端口。
 */
public interface ObjectStore {

    /** 读取对象内容；对象不存在抛异常（与 MinIO get 语义一致）。 */
    InputStream getObject(String key);

    /** 幂等删除（S3 removeObject 对不存在对象不报错）。 */
    void deleteObject(String key);
}
