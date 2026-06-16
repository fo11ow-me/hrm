package com.qiujie.spi;

import java.util.Collections;
import java.util.Map;

/**
 * 分片上传完成后的处理策略。
 * 知识库创建 KnowledgeDocument + 触发 ETL，通用文档创建 Docs + 压缩去重。
 */
public interface UploadCompletionHandler {

    /** 存储路径前缀 */
    String getStoragePrefix();

    /**
     * 秒传检测：已存在相同 hash 的文档时返回其信息。
     * @return 存在则返回 {instantUpload:true, ...}，不存在返回 null
     */
    default Map<String, Object> checkDedup(String fileHash) {
        return Collections.emptyMap();
    }

    /**
     * 分片合并完成后执行的业务逻辑。
     * @param mergedKey  合并后对象的存储 key
     * @param session    上传会话信息
     * @return 返回给客户端的额外字段，可为 null
     */
    Map<String, Object> onComplete(String mergedKey, UploadSessionInfo session);
}
