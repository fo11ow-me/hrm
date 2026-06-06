package com.qiujie.controller;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.FileTaskService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/file-task")
public class FileTaskController {

    @Autowired
    private FileTaskService fileTaskService;

    @ApiOperation("SSE 订阅任务状态更新")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return this.fileTaskService.subscribeSse();
    }

    @ApiOperation("查询文件任务")
    @GetMapping
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String taskType,
                            String module) {
        return this.fileTaskService.list(current, size, taskType, module);
    }

    @ApiOperation("查询文件任务详情")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Long id) {
        return this.fileTaskService.query(id);
    }

    @ApiOperation("查询导入错误明细")
    @GetMapping("/{id}/errors")
    public ResponseDTO queryErrors(@PathVariable Long id,
                                   @RequestParam(defaultValue = "1") Integer current,
                                   @RequestParam(defaultValue = "10") Integer size) {
        return this.fileTaskService.queryErrors(id, current, size);
    }

    @ApiOperation("下载任务文件")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, @RequestParam(defaultValue = "RESULT") String fileType,
                         HttpServletResponse response) throws IOException {
        this.fileTaskService.download(id, fileType, response);
    }
}
