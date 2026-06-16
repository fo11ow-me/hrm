package com.qiujie.knowledge.controller;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.dto.QaRequest;
import com.qiujie.knowledge.service.QaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 知识库 RAG 问答控制器。
 */
@RestController
@RequestMapping("/knowledge/qa")
public class QaController {

    @Autowired
    private QaService qaService;

    /**
     * 标准问答。
     */
    @PostMapping("/ask")
    public ResponseDTO ask(@RequestBody QaRequest request) {
        return qaService.ask(request);
    }

    /**
     * SSE 流式问答。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@RequestBody QaRequest request) {
        return qaService.streamAsk(request);
    }
}
