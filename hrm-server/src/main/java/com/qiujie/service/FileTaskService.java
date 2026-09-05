package com.qiujie.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.FileTaskErrorExportRow;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.enums.TaskFileTypeEnum;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.enums.TaskTypeEnum;
import com.qiujie.entity.FileTask;
import com.qiujie.filetask.ArtifactStore;
import com.qiujie.entity.FileTaskError;
import com.qiujie.mapper.FileTaskMapper;
import com.qiujie.storage.MinioStorageService;
import com.qiujie.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileTaskService extends ServiceImpl<FileTaskMapper, FileTask>
        implements com.qiujie.filetask.TaskRepository {

    private static final int ERROR_EXPORT_PAGE_SIZE = 1000;
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "hrm";

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private ArtifactStore artifactStore;

    @Autowired
    private FileTaskMapper fileTaskMapper;

    @Autowired
    private FileTaskErrorService fileTaskErrorService;

    @Autowired
    private SseService sseService;

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public FileTask create(TaskTypeEnum taskType, TaskModuleEnum module, String fileName, String sourceFilePath,
                           String queryParams, Integer operatorId) {
        return createTask(taskType, module, fileName, sourceFilePath, queryParams, operatorId);
    }

    @Override
    public FileTask getById(Long id) {
        return super.getById(id);
    }

    public FileTask createTask(TaskTypeEnum taskType, TaskModuleEnum module, String fileName, String sourceFilePath,
                               String queryParams, Integer operatorId) {
        FileTask fileTask = new FileTask()
                .setTaskType(taskType)
                .setModule(module)
                .setStatus(TaskStatusEnum.PENDING)
                .setFileName(fileName)
                .setSourceFilePath(sourceFilePath)
                .setQueryParams(queryParams)
                .setTotalCount(0)
                .setProcessedCount(0)
                .setSuccessCount(0)
                .setFailCount(0)
                .setOperatorId(operatorId);
        save(fileTask);
        sseService.emit(fileTask);
        return fileTask;
    }

    public ResponseDTO list(Integer current, Integer size, String taskType, String module) {
        QueryWrapper<FileTask> queryWrapper = new QueryWrapper<>();
        Integer operatorId = getCurrentOperatorId();
        if (operatorId != null) {
            queryWrapper.eq("operator_id", operatorId);
        }
        if (taskType != null && !"".equals(taskType)) {
            queryWrapper.eq("task_type", taskType);
        }
        if (module != null && !"".equals(module)) {
            queryWrapper.eq("module", module);
        }
        queryWrapper.orderByDesc("id");
        IPage<FileTask> page = page(new Page<>(current, size), queryWrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return Response.success(map);
    }

    public ResponseDTO query(Long id) {
        FileTask fileTask = getById(id);
        if (fileTask == null) {
            return Response.error("任务不存在");
        }
        if (!canAccess(fileTask)) {
            return Response.error("无权访问该任务");
        }
        return Response.success(fileTask);
    }

    public ResponseDTO queryErrors(Long taskId, Integer current, Integer size) {
        FileTask fileTask = getById(taskId);
        if (fileTask == null) {
            return Response.error("任务不存在");
        }
        if (!canAccess(fileTask)) {
            return Response.error("无权访问该任务");
        }
        QueryWrapper<FileTaskError> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).orderByAsc("row_num");
        IPage<FileTaskError> page = fileTaskErrorService.page(new Page<>(current, size), queryWrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return Response.success(map);
    }

    public void markRunning(Long id) {
        updateById(new FileTask()
                .setId(id)
                .setStatus(TaskStatusEnum.RUNNING)
                .setStartTime(Timestamp.valueOf(LocalDateTime.now())));
        pushTaskEvent(id);
    }

    /**
     * 原子认领待执行任务，避免同一任务被多个线程重复执行。
     */
    public boolean claimRunning(Long id) {
        boolean claimed = fileTaskMapper.claimRunning(id) == 1;
        if (claimed) {
            pushTaskEvent(id);
        }
        return claimed;
    }

    public void increaseProgress(Long id, int total, int processed, int success, int fail) {
        fileTaskMapper.increaseProgress(id, total, processed, success, fail);
        pushTaskEvent(id);
    }

    private void pushTaskEvent(Long id) {
        FileTask task = getById(id);
        if (task != null) {
            sseService.emit(task);
        }
    }

    public void setTotalCount(Long id, int total) {
        updateById(new FileTask().setId(id).setTotalCount(total));
    }

    public void setResultFile(Long id, String resultFilePath) {
        updateById(new FileTask().setId(id).setResultFilePath(resultFilePath));
    }

    public void setErrorFile(Long id, String errorFilePath) {
        updateById(new FileTask().setId(id).setErrorFilePath(errorFilePath));
    }

    public SseEmitter subscribeSse() {
        Integer operatorId = getCurrentOperatorId();
        if (operatorId == null) {
            return null;
        }
        return sseService.subscribe(operatorId);
    }

    public void finish(Long id, TaskStatusEnum status) {
        updateById(new FileTask()
                .setId(id)
                .setStatus(status)
                .setFinishTime(Timestamp.valueOf(LocalDateTime.now())));
        pushTaskEvent(id);
    }

    @Override
    public void fail(Long id, Exception e) {
        fail(id, e == null ? null : e.getMessage());
    }


    public void fail(Long id, String message) {
        if (message != null && message.length() > 1000) {
            message = message.substring(0, 1000);
        }
        updateById(new FileTask()
                .setId(id)
                .setStatus(TaskStatusEnum.FAILED)
                .setFailReason(message)
                .setFinishTime(Timestamp.valueOf(LocalDateTime.now())));
        pushTaskEvent(id);
    }

    public File buildTaskFile(String subDir, String originalFilename) {
        return artifactStore.createTaskFile(subDir, originalFilename);
    }

    /** 将本地临时文件上传至 MinIO，返回存储 key。 */
    public String uploadToMinio(File file, String subDir) {
        return artifactStore.upload(file, subDir);
    }

    public void generateErrorFile(Long taskId) {
        File errorFile = artifactStore.createTaskFile("task-error", "import-errors.xlsx");
        ExcelWriter excelWriter = EasyExcel.write(errorFile, FileTaskErrorExportRow.class).build();
        try {
            WriteSheet writeSheet = EasyExcel.writerSheet("errors").build();
            long lastId = 0;
            while (true) {
                QueryWrapper<FileTaskError> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("task_id", taskId).gt("id", lastId)
                        .orderByAsc("id").last("limit " + ERROR_EXPORT_PAGE_SIZE);
                List<FileTaskError> errors = fileTaskErrorService.list(queryWrapper);
                if (errors.isEmpty()) {
                    break;
                }
                List<FileTaskErrorExportRow> rows = errors.stream()
                        .map(error -> new FileTaskErrorExportRow()
                                .setRowNum(error.getRowNum())
                                .setRawData(error.getRawData())
                                .setErrorMessage(error.getErrorMessage()))
                        .collect(Collectors.toList());
                excelWriter.write(rows, writeSheet);
                lastId = errors.get(errors.size() - 1).getId();
            }
        } finally {
            excelWriter.finish();
        }
        String key = artifactStore.upload(errorFile, "task-error");
        setErrorFile(taskId, key);
    }

    public void download(Long id, String fileType, HttpServletResponse response) throws IOException {
        FileTask fileTask = getById(id);
        if (fileTask == null) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "任务不存在");
            return;
        }
        if (!canAccess(fileTask)) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "无权访问该任务");
            return;
        }
        String key = resolveDownloadKey(fileTask, fileType);
        if (key == null || "".equals(key)) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }
        if (!artifactStore.exists(key)) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "文件不存在或已被清理");
            return;
        }
        String downloadName = resolveDownloadName(fileTask, fileType);
        response.addHeader("Content-Type", "application/octet-stream;charset=utf-8");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(downloadName, StandardCharsets.UTF_8));
        try (InputStream in = artifactStore.open(key);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredTaskFiles() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(7);
        QueryWrapper<FileTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("create_time", Timestamp.valueOf(expireTime));
        List<FileTask> expiredTasks = list(queryWrapper);
        for (FileTask task : expiredTasks) {
            artifactStore.delete(task.getSourceFilePath());
            artifactStore.delete(task.getResultFilePath());
            artifactStore.delete(task.getErrorFilePath());
            fileTaskErrorService.remove(new QueryWrapper<FileTaskError>().eq("task_id", task.getId()));
            removeById(task.getId());
        }
    }

    public void deleteSourceFile(Long taskId) {
        FileTask task = getById(taskId);
        if (task != null) {
            artifactStore.delete(task.getSourceFilePath());
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(JSON.toJSONString(Response.error(message)));
    }

    private boolean canAccess(FileTask fileTask) {
        Integer operatorId = getCurrentOperatorId();
        return operatorId == null || fileTask.getOperatorId() == null || operatorId.equals(fileTask.getOperatorId());
    }

    private Integer getCurrentOperatorId() {
        return securityUtil.getCurrentOperatorId();
    }

    private String resolveDownloadKey(FileTask fileTask, String fileType) {
        if (TaskFileTypeEnum.SOURCE.getValue().equalsIgnoreCase(fileType)) {
            return fileTask.getSourceFilePath();
        }
        if (TaskFileTypeEnum.ERROR.getValue().equalsIgnoreCase(fileType)) {
            return fileTask.getErrorFilePath();
        }
        return fileTask.getResultFilePath();
    }

    private String resolveDownloadName(FileTask fileTask, String fileType) {
        if (TaskFileTypeEnum.ERROR.getValue().equalsIgnoreCase(fileType)) {
            return "import-errors.xlsx";
        }
        String name = fileTask.getFileName();
        return name != null && !"".equals(name) ? name : "download";
    }
}
