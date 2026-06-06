package com.qiujie.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.FileTaskErrorExportRow;
import com.qiujie.enums.TaskFileTypeEnum;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.enums.TaskStatusEnum;
import com.qiujie.enums.TaskTypeEnum;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.FileTask;
import com.qiujie.entity.FileTaskError;
import com.qiujie.mapper.FileTaskMapper;
import com.qiujie.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
public class FileTaskService extends ServiceImpl<FileTaskMapper, FileTask> {

    private static final int ERROR_EXPORT_PAGE_SIZE = 1000;

    @Value("${file-path}")
    private String filePath;

    @Autowired
    private FileTaskMapper fileTaskMapper;

    @Autowired
    private FileTaskErrorService fileTaskErrorService;

    @Autowired
    private FileTaskSseService sseService;

    @Autowired
    private SecurityUtil securityUtil;

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

    public void fail(Long id, Exception e) {
        fail(id, e.getMessage());
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
        String extName = FileUtil.extName(originalFilename);
        String filename = IdUtil.fastSimpleUUID();
        if (extName != null && !"".equals(extName)) {
            filename = filename + "." + extName;
        }
        File dir = new File(filePath, subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, filename);
    }

    public void generateErrorFile(Long taskId) {
        File errorFile = buildTaskFile("task-error", "import-errors.xlsx");
        ExcelWriter excelWriter = EasyExcel.write(errorFile, FileTaskErrorExportRow.class).build();
        try {
            WriteSheet writeSheet = EasyExcel.writerSheet("errors").build();
            long lastId = 0;
            while (true) {
                // 游标分页，避免深 OFFSET 导致的性能下降
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
        setErrorFile(taskId, errorFile.getAbsolutePath());
    }

    public void download(Long id, String fileType, HttpServletResponse response) throws IOException {
        FileTask fileTask = getById(id);
        if (fileTask == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!canAccess(fileTask)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String path = resolveDownloadPath(fileTask, fileType);
        if (path == null || "".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String downloadName = fileTask.getFileName();
        if (TaskFileTypeEnum.ERROR.getValue().equalsIgnoreCase(fileType)) {
            downloadName = "import-errors.xlsx";
        } else if (downloadName == null || "".equals(downloadName)) {
            downloadName = file.getName();
        }
        response.addHeader("Content-Type", "application/octet-stream;charset=utf-8");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(downloadName, StandardCharsets.UTF_8));
        try (FileInputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        }
    }

    private String resolveDownloadPath(FileTask fileTask, String fileType) {
        if (TaskFileTypeEnum.SOURCE.getValue().equalsIgnoreCase(fileType)) {
            return fileTask.getSourceFilePath();
        }
        if (TaskFileTypeEnum.ERROR.getValue().equalsIgnoreCase(fileType)) {
            return fileTask.getErrorFilePath();
        }
        return fileTask.getResultFilePath();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredTaskFiles() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(7);
        QueryWrapper<FileTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("create_time", Timestamp.valueOf(expireTime));
        List<FileTask> expiredTasks = list(queryWrapper);
        for (FileTask task : expiredTasks) {
            deleteIfExists(task.getSourceFilePath());
            deleteIfExists(task.getResultFilePath());
            deleteIfExists(task.getErrorFilePath());
            // 删除关联的错误明细和任务记录，防止表无限增长
            fileTaskErrorService.remove(new QueryWrapper<FileTaskError>().eq("task_id", task.getId()));
            removeById(task.getId());
        }
    }

    private void deleteIfExists(String path) {
        if (path == null || "".equals(path)) {
            return;
        }
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    private boolean canAccess(FileTask fileTask) {
        Integer operatorId = getCurrentOperatorId();
        return operatorId == null || fileTask.getOperatorId() == null || operatorId.equals(fileTask.getOperatorId());
    }

    private Integer getCurrentOperatorId() {
        return securityUtil.getCurrentOperatorId();
    }
}
