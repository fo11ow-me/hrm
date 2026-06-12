package com.qiujie.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Docs;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.DocsMapper;
import com.qiujie.util.HutoolExcelUtil;
import com.qiujie.util.StorageCompressor;
import com.qiujie.vo.StaffDocsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.rmi.ServerError;
import java.rmi.ServerException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author qiujie
 * @Date 2022/2/24
 * @Version 1.0
 */
@Service
public class DocsService extends ServiceImpl<DocsMapper, Docs> {

    private static final Logger log = LoggerFactory.getLogger(DocsService.class);

    @Value("${file-path}")
    private String filePath;

    @Autowired
    private OssService ossService;

    @Autowired
    private DocsMapper docsMapper;

    /**
     * 允许上传的文件类型白名单
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",  // 文档
        "jpg", "jpeg", "png", "gif", "bmp", "svg",           // 图片
        "txt", "csv", "xml", "json",                         // 文本
        "zip", "rar", "7z", "tar", "gz"                      // 压缩包
    );

    @Value("${docs.max-file-size:52428800}")
    private long maxFileSize;  // 默认 50MB

    /**
     * document upload
     *
     * @param uploadFile
     * @param id
     * @return
     * @throws IOException
     */
    public ResponseDTO upload(MultipartFile uploadFile, Integer id) throws IOException {
        // 判断上传的文件是否为空
        if (uploadFile.isEmpty()) {
            return Response.error(BusinessStatusEnum.FILE_NOT_EXIST);
        }

        // 显式验证文件大小
        if (uploadFile.getSize() > maxFileSize) {
            return Response.error("文件大小超过限制，最大允许 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String originalFilename = uploadFile.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);

        // 文件类型白名单验证
        if (!ALLOWED_EXTENSIONS.contains(extName.toLowerCase())) {
            return Response.error("不支持的文件类型: " + extName);
        }

        // 一次性读取全部字节，避免多次 IO
        byte[] rawBytes = uploadFile.getBytes();
        String filename = IdUtil.fastSimpleUUID().substring(2, 22) + "." + extName;
        String md5 = SecureUtil.md5(new ByteArrayInputStream(rawBytes));
        List<Docs> docsList = list(new QueryWrapper<Docs>().eq("md5", md5));

        StorageCompressor.CompressionResult result;
        long storedSize;
        // 若文件已经存在，则不用上传，复用已有文件信息
        if (!docsList.isEmpty()) {
            Docs existing = docsList.get(0);
            filename = existing.getName();
            storedSize = existing.getStoredSize() != null ? existing.getStoredSize() : rawBytes.length;
            // 如果启用了 OSS 但文件不在 OSS，则补传（兼容迁移前数据）
            if (ossService != null && ossService.isEnabled() && !ossService.exists(filename)) {
                result = new StorageCompressor.CompressionResult(rawBytes, false);
                storedSize = rawBytes.length;
                try {
                    ossService.put(filename, new ByteArrayInputStream(rawBytes), uploadFile.getContentType());
                } catch (Exception e) {
                    log.error("OSS文件上传失败, filename={}", filename, e);
                    throw new ServiceException(BusinessStatusEnum.FILE_UPLOAD_ERROR.getCode(),
                            BusinessStatusEnum.FILE_UPLOAD_ERROR.getMessage() + ": " + e.getMessage());
                }
            } else {
                result = new StorageCompressor.CompressionResult(rawBytes,
                        existing.getCompressed() != null && existing.getCompressed() == 1);
            }
        } else {
            result = StorageCompressor.tryCompress(rawBytes, extName.toLowerCase());
            storedSize = (long) result.bytes.length;
            try {
                if (ossService != null && ossService.isEnabled()) {
                    ossService.put(filename, new ByteArrayInputStream(result.bytes), uploadFile.getContentType());
                } else {
                    File fold = new File(filePath);
                    if (!fold.exists() && !fold.mkdirs()) {
                        log.error("文件上传失败: 无法创建目录, filePath={}", filePath);
                        throw new ServiceException(BusinessStatusEnum.FILE_WRITE_ERROR.getCode(),
                                "无法创建目录: " + filePath);
                    }
                    FileUtil.writeBytes(result.bytes, new File(filePath, filename));
                }
                if (result.compressed) {
                    log.info("文件压缩存储: {} ({} -> {} bytes)", filename, rawBytes.length, result.bytes.length);
                }
            } catch (Exception e) {
                log.error("文件上传失败, filePath={}, filename={}", filePath, filename, e);
                throw new ServiceException(BusinessStatusEnum.FILE_UPLOAD_ERROR.getCode(),
                        BusinessStatusEnum.FILE_UPLOAD_ERROR.getMessage() + ": " + e.getMessage());
            }
        }
        // 将文件数据保存到数据库
        Docs docs = new Docs().setName(filename)
                .setStaffId(id)
                .setType(extName)
                .setOldName(originalFilename)
                .setMd5(md5)
                .setSize((long) rawBytes.length / 1024) // 原始大小 KB
                .setStoredSize(storedSize)
                .setCompressed(result.compressed ? 1 : 0);
        if (!save(docs)) {
            return Response.error();
        }
        return Response.success("文件上传成功！", docs);
    }


    /**
     * 在文件下载以及数据导出时，响应对象是可以不用作为方法返回值返回的，其在方法执行时已经开始输出，
     * 且其无法与@RestController配合，以JSON格式返回给前端；如果返回响应对象，后端会抛出异常
     *
     * @param filename
     * @param response
     * @return
     * @throws IOException
     */
    public void download(String filename, HttpServletResponse response) throws IOException {
        response.addHeader("Content-Type", "application/octet-stream;charset=utf-8");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        // 流式下载：不将整个文件加载到内存
        InputStream rawStream;
        if (ossService != null && ossService.isEnabled()) {
            if (!ossService.exists(filename)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            rawStream = ossService.get(filename);
        } else {
            File file = resolveStoredFile(filename);
            if (file == null || !file.exists() || !file.isFile()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            rawStream = new FileInputStream(file);
            response.addHeader("Content-Length", String.valueOf(file.length()));
        }

        try (InputStream in = rawStream) {
            // 探测是否压缩数据
            byte[] header = StorageCompressor.peekMagicBytes(in);
            OutputStream out = response.getOutputStream();

            if (header.length == 4
                    && header[0] == (byte) 0x28 && header[1] == (byte) 0xB5
                    && header[2] == 0x2F && header[3] == (byte) 0xFD) {
                // 压缩数据：流式解压输出
                // 查询原始大小用于 Content-Length
                Docs docs = getOne(new QueryWrapper<Docs>().eq("name", filename));
                if (docs != null && docs.getSize() != null) {
                    response.setHeader("Content-Length", String.valueOf(docs.getSize() * 1024));
                }
                // 拼接回 magic bytes + 剩余流，再解压
                try (ByteArrayInputStream headerStream = new ByteArrayInputStream(header);
                     InputStream fullStream = new java.io.SequenceInputStream(headerStream, in);
                     InputStream decompressed = StorageCompressor.decompressStream(fullStream)) {
                    IoUtil.copy(decompressed, out, 8192);
                }
            } else {
                // 未压缩数据：拼接 header + 剩余流直接输出
                out.write(header);
                IoUtil.copy(in, out, 8192);
            }
            out.flush();
        }
    }

    private File resolveStoredFile(String filename) throws IOException {
        File baseDir = new File(filePath).getCanonicalFile();
        File file = new File(baseDir, filename).getCanonicalFile();
        String basePath = baseDir.getPath();
        if (!file.getPath().equals(basePath) && file.getPath().startsWith(basePath + File.separator)) {
            return file;
        }
        return null;
    }


    public ResponseDTO add(Docs docs) {
        if (save(docs)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO delete(Integer id) {
        if (removeById(id)) {
            return Response.success();
        }
        return Response.error();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO edit(Docs docs) {
        if (updateById(docs)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        Docs docs = getById(id);
        if (docs != null) {
            return Response.success(docs);
        }
        return Response.error();
    }


    public ResponseDTO list(Integer current, Integer size, String oldName, String staffName) {
        if (oldName == null) {
            oldName = "";
        }
        if (staffName == null) {
            staffName = "";
        }
        IPage<StaffDocsVO> config = new Page<>(current, size);
        IPage<StaffDocsVO> page = this.docsMapper.listStaffDocsVO(config, oldName, staffName);
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return Response.success(map);
    }

    /**
     * 在文件下载以及数据导出时，响应对象是可以不用作为方法返回值返回的，其在方法执行时已经开始输出，
     * 且其无法与@RestController配合，以JSON格式返回给前端；如果返回响应对象，后端会抛出异常
     *
     * @param response
     * @return
     * @throws IOException
     */
    public void export(HttpServletResponse response, String filename) throws IOException {
        List<Docs> list = list();
        HutoolExcelUtil.writeExcel(response, list, filename, Docs.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO imp(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        List<Docs> list = HutoolExcelUtil.readExcel(inputStream, 1, Docs.class);
        // IService接口中的方法.批量插入数据
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 每日凌晨 4:00 清理无 DB 引用的孤儿文件。
     * 排除 task- 子目录（已有独立的 7 天清理逻辑）。
     * 文件需满足：最后修改时间超过 24 小时 且 文件名不在 sys_docs 表中。
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanOrphanDocsFiles() {
        Set<String> dbFilenames = list().stream()
                .map(Docs::getName)
                .collect(Collectors.toSet());

        long cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000;

        // 本地文件系统：遍历 file-path 下普通文件，排除 task- 子目录
        File baseDir = new File(filePath);
        File[] files = baseDir.listFiles(f -> f.isFile());
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > cutoff) {
                    continue;
                }
                if (dbFilenames.contains(file.getName())) {
                    continue;
                }
                if (file.delete()) {
                    log.info("清理孤儿文件: {} ({} bytes)", file.getName(), file.length());
                } else {
                    log.error("清理孤儿文件失败: {}", file.getAbsolutePath());
                }
            }
        }

        // OSS：遍历所有 object，清理无 DB 引用且 over 24h 的
        if (ossService != null && ossService.isEnabled()) {
            for (String key : ossService.listKeys()) {
                if (dbFilenames.contains(key)) {
                    continue;
                }
                Date lastModified = ossService.getLastModified(key);
                if (lastModified != null && lastModified.getTime() > cutoff) {
                    continue;
                }
                ossService.delete(key);
                log.info("清理OSS孤儿对象: {}", key);
            }
        }
    }


}
