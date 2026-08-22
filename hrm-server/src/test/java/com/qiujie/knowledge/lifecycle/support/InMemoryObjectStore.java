package com.qiujie.knowledge.lifecycle.support;

import com.qiujie.knowledge.lifecycle.port.ObjectStore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ObjectStore 内存假适配器：getObject 对缺失 key 抛异常（与生产 MinIO 语义一致）。
 */
public class InMemoryObjectStore implements ObjectStore {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public InputStream getObject(String key) {
        byte[] bytes = objects.get(key);
        if (bytes == null) {
            throw new RuntimeException("对象不存在: " + key);
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void deleteObject(String key) {
        objects.remove(key);
    }

    /** 测试辅助：预置对象内容。 */
    public void put(String key, byte[] bytes) {
        objects.put(key, bytes);
    }

    /** 测试辅助：对象是否存在。 */
    public boolean has(String key) {
        return objects.containsKey(key);
    }
}
