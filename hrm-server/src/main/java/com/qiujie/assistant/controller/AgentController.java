package com.qiujie.assistant.controller;

import com.qiujie.assistant.dto.AgentChatRequest;
import com.qiujie.assistant.entity.AgentMessage;
import com.qiujie.assistant.entity.AgentSession;
import com.qiujie.assistant.service.AgentService;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI Agent 控制器：SSE 流式对话 + 会话管理。
 */
@RestController
@RequestMapping("/assistant")
public class AgentController {

    @Autowired
    private AgentService agentService;

    /**
     * 同步对话。
     */
    @PostMapping("/chat")
    public ResponseDTO chat(@RequestBody AgentChatRequest request) {
        return agentService.chatSync(request);
    }

    /**
     * SSE 流式对话。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AgentChatRequest request) {
        return agentService.chat(request);
    }

    /**
     * 获取会话列表。
     */
    @GetMapping("/conversations")
    public ResponseDTO listSessions() {
        List<AgentSession> sessions = agentService.listSessions();
        return Response.success(sessions);
    }

    /**
     * 获取单个会话详情。
     */
    @GetMapping("/conversations/{id}")
    public ResponseDTO getSession(@PathVariable Long id) {
        AgentSession session = agentService.getSession(id);
        if (session == null) {
            return com.qiujie.dto.Response.error("会话不存在");
        }
        return Response.success(session);
    }

    /**
     * 获取会话消息。
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseDTO listMessages(@PathVariable Long id) {
        List<AgentMessage> messages = agentService.listMessages(id);
        return Response.success(messages);
    }

    /**
     * 切换会话模式。
     */
    @PutMapping("/conversations/{id}/mode")
    public ResponseDTO switchMode(@PathVariable Long id, @RequestBody Map<String, String> body) {
        agentService.switchMode(id, body.get("mode"));
        return Response.success();
    }

    /**
     * 删除会话。
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseDTO deleteSession(@PathVariable Long id) {
        agentService.deleteSession(id);
        return Response.success();
    }
}
