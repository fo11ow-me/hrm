package com.qiujie.knowledge.lifecycle.port;

import com.qiujie.storage.MinioStorageService;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * ObjectStore 生产适配器：委托现有 MinioStorageService（get/delete 签名现成吻合）。
 */
@Component
public class MinioObjectStore implements ObjectStore {

    private final MinioStorageService storageService;

    public MinioObjectStore(MinioStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public InputStream getObject(String key) {
        return storageService.get(key);
    }

    @Override
    public void deleteObject(String key) {
        storageService.delete(key);
    }
}
