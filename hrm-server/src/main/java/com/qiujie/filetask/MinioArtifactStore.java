package com.qiujie.filetask;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.qiujie.storage.MinioStorageService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 MinIO + 本地临时目录的任务文件介质适配器。
 * <p>
 * 兼容旧数据：历史任务记录使用本地绝对路径，新任务统一使用对象存储 key。
 * 路径格式判定规则：包含路径分隔符或以 / 开头 → 本地路径；否则 → MinIO key。
 * </p>
 */
@Component
public class MinioArtifactStore implements ArtifactStore {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "hrm";

    private final MinioStorageService storageService;

    public MinioArtifactStore(MinioStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public boolean isRemote(String path) {
        return path != null && !path.contains(File.separator) && !path.startsWith("/");
    }

    @Override
    public File resolveSource(String source) {
        if (source == null || !isRemote(source)) {
            return new File(source);
        }
        File tempFile = new File(TEMP_DIR, source);
        tempFile.getParentFile().mkdirs();
        try (InputStream in = storageService.get(source)) {
            FileUtil.writeFromStream(in, tempFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download source file from MinIO: " + source, e);
        }
        return tempFile;
    }

    @Override
    public File createTaskFile(String subDir, String originalFilename) {
        String extName = FileUtil.extName(originalFilename);
        String filename = IdUtil.fastSimpleUUID();
        if (extName != null && !"".equals(extName)) {
            filename = filename + "." + extName;
        }
        File dir = new File(TEMP_DIR, subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, filename);
    }

    @Override
    public String upload(File file, String subDir) {
        String key = subDir + "/" + file.getName();
        storageService.put(key, FileUtil.readBytes(file));
        file.delete();
        return key;
    }

    @Override
    public void delete(String path) {
        if (path == null || "".equals(path)) {
            return;
        }
        if (!isRemote(path)) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
            return;
        }
        if (storageService.exists(path)) {
            storageService.delete(path);
        }
    }

    @Override
    public boolean exists(String path) {
        if (path == null || "".equals(path)) {
            return false;
        }
        if (!isRemote(path)) {
            File file = new File(path);
            return file.exists() && file.isFile();
        }
        return storageService.exists(path);
    }

    @Override
    public InputStream open(String path) throws IOException {
        if (!isRemote(path)) {
            return java.nio.file.Files.newInputStream(new File(path).toPath());
        }
        return storageService.get(path);
    }
}
