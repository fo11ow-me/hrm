package com.qiujie.service;

import cn.hutool.core.io.FileUtil;
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
import com.qiujie.vo.StaffDocsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.rmi.ServerError;
import java.rmi.ServerException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author qiujie
 * @Date 2022/2/24
 * @Version 1.0
 */
@Service
public class DocsService extends ServiceImpl<DocsMapper, Docs> {

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

    /**
     * 最大文件大小 (20MB)
     */
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

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
        if (uploadFile.getSize() > MAX_FILE_SIZE) {
            return Response.error("文件大小超过限制，最大允许 20MB");
        }

        String originalFilename = uploadFile.getOriginalFilename(); // 获取文件的原名称
        String extName = FileUtil.extName(originalFilename); // 获取文件的后缀名

        // 文件类型白名单验证
        if (!ALLOWED_EXTENSIONS.contains(extName.toLowerCase())) {
            return Response.error("不支持的文件类型: " + extName);
        }

        String filename = IdUtil.fastSimpleUUID().substring(2, 22) + "." + extName; // 文件名
        // 获取文件的md5信息
        String md5 = SecureUtil.md5(uploadFile.getInputStream());
        List<Docs> docsList = list(new QueryWrapper<Docs>().eq("md5", md5));
        // 若文件已经存在，则不用上传
        if (!docsList.isEmpty()) {
            filename = docsList.get(0).getName();
            // 如果启用了 OSS 但文件不在 OSS，则补传（兼容迁移前数据）
            if (ossService != null && ossService.isEnabled() && !ossService.exists(filename)) {
                try {
                    ossService.put(filename, uploadFile.getInputStream(), uploadFile.getContentType());
                } catch (Exception e) {
                    throw new ServiceException(BusinessStatusEnum.FILE_UPLOAD_ERROR);
                }
            }
        } else {
            try {
                if (ossService != null && ossService.isEnabled()) {
                    ossService.put(filename, uploadFile.getInputStream(), uploadFile.getContentType());
                } else {
                    File fold = new File(filePath);
                    if (!fold.exists()) {
                        fold.mkdirs();
                    }
                    File file = new File(filePath, filename);
                    uploadFile.transferTo(file);
                }
            } catch (Exception e) {
                throw new ServiceException(BusinessStatusEnum.FILE_UPLOAD_ERROR);
            }
        }
        // 将文件数据保存到数据库
        Docs docs = new Docs().setName(filename)
                .setStaffId(id) // 文件上传者
                .setType(extName)
                .setOldName(originalFilename)
                .setMd5(md5)
                .setSize(uploadFile.getSize() / 1024); // KB
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

        if (ossService != null && ossService.isEnabled()) {
            if (!ossService.exists(filename)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            try (InputStream in = ossService.get(filename);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
        } else {
            File file = resolveStoredFile(filename);
            if (file == null || !file.exists() || !file.isFile()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            OutputStream out = response.getOutputStream();
            out.write(FileUtil.readBytes(file));
            out.flush();
            out.close();
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


}
