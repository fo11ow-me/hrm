package com.qiujie.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class OssService {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${oss.bucket-name}")
    private String bucketName;

    private OSS client;
    private boolean enabled;

    @PostConstruct
    void init() {
        enabled = endpoint != null && !endpoint.isEmpty();
        if (enabled) {
            client = new com.aliyun.oss.OSSClientBuilder()
                    .build(endpoint, accessKeyId, accessKeySecret);
        }
    }

    @PreDestroy
    void shutdown() {
        if (client != null) {
            client.shutdown();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public InputStream get(String key) {
        OSSObject object = client.getObject(bucketName, key);
        return object.getObjectContent();
    }

    public void put(String key, InputStream inputStream, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        client.putObject(bucketName, key, inputStream, metadata);
    }

    public void put(String key, byte[] bytes) {
        client.putObject(bucketName, key, new ByteArrayInputStream(bytes));
    }

    public void delete(String key) {
        client.deleteObject(bucketName, key);
    }

    public boolean exists(String key) {
        return client.doesObjectExist(bucketName, key);
    }
}
