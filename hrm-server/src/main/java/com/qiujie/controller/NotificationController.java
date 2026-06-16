package com.qiujie.controller;

import com.qiujie.service.SseService;
import com.qiujie.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通知 SSE 订阅端点。
 *
 * @author qiujie
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private SseService sseService;

    @Autowired
    private SecurityUtil securityUtil;

    @Operation(summary = "订阅通知（SSE）")
    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        Integer staffId = securityUtil.getCurrentOperatorId();
        return sseService.subscribe(staffId);
    }
}
