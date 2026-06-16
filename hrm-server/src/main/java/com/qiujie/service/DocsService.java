package com.qiujie.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Docs;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.mapper.DocsMapper;
import com.qiujie.util.HutoolExcelUtil;
import com.qiujie.util.StorageCompressor;
import com.qiujie.storage.MinioStorageService;
import com.qiujie.vo.StaffDocsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private DocsMapper docsMapper;

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
        if (!storageService.exists(filename)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        InputStream rawStream = storageService.get(filename);

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

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp");

    /**
     * 头像上传（小文件直传 MinIO），复用文件名前缀兼容旧逻辑
     */
    public ResponseDTO upload(MultipartFile file, Integer staffId) throws IOException {
        if (file.isEmpty()) {
            return Response.error(BusinessStatusEnum.FILE_NOT_EXIST);
        }
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extName.toLowerCase())) {
            return Response.error("不支持的文件类型: " + extName);
        }
        String filename = cn.hutool.core.util.IdUtil.fastSimpleUUID().substring(2, 22) + "." + extName;
        storageService.put(filename, file.getBytes());
        Map<String, Object> result = new HashMap<>();
        result.put("name", filename);
        return Response.success(result);
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

        for (String key : storageService.listKeys()) {
            if (dbFilenames.contains(key)) {
                continue;
            }
            Date lastModified = storageService.getLastModified(key);
            if (lastModified != null && lastModified.getTime() > cutoff) {
                continue;
            }
            storageService.delete(key);
            log.info("清理孤儿文件: {}", key);
        }
    }


}
