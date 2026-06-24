package com.qiujie.assistant.service;

import com.qiujie.assistant.AssistantTools;
import com.qiujie.assistant.dto.AgentChatRequest;
import com.qiujie.assistant.entity.AgentMessage;
import com.qiujie.assistant.entity.AgentSession;
import com.qiujie.assistant.mapper.AgentMessageMapper;
import com.qiujie.assistant.mapper.AgentSessionMapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Agent 引擎：ReAct 循环（思考 → 工具调用 → 生成回复）。
 * 支持 CHAT / KB_SEARCH 双模式，同一会话内动态切换。
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    @Autowired
    private AgentSessionMapper sessionMapper;

    @Autowired
    private AgentMessageMapper messageMapper;

    @Autowired
    private AgentMemoryService memoryService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private AssistantTools assistantTools;

    private final ChatClient chatClient;

    public AgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 同步对话。
     */
    @Transactional
    public ResponseDTO chatSync(AgentChatRequest request) {
        Integer staffId = securityUtil.getCurrentOperatorId();
        AgentSession session = getOrCreateSession(request, staffId);

        // 保存用户消息
        AgentMessage userMsg = new AgentMessage()
                .setSessionId(session.getId()).setRole("user")
                .setContent(request.getMessage()).setTokenCount(estimateTokens(request.getMessage()))
                .setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        String answer = chatClient.prompt()
                .user(request.getMessage())
                .tools(assistantTools)
                .call()
                .content();

        // 保存应答
        AgentMessage assistantMsg = new AgentMessage()
                .setSessionId(session.getId()).setRole("assistant")
                .setContent(answer).setTokenCount(estimateTokens(answer))
                .setCreatedAt(LocalDateTime.now());
        messageMapper.insert(assistantMsg);
        memoryService.afterMessage(session);

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", session.getId());
        data.put("answer", answer);
        data.put("suggestions", List.of("查询我的考勤", "请假流程是什么", "本月薪资明细"));
        return Response.success(data);
    }

    /**
     * SSE 流式对话 — 后台线程调 ChatClient.call()（含 Tool），再逐字推流。
     * 注：Spring AI 1.0.0-M6 MethodToolCallback 不支持空 toolInput，.stream() 模式
     * 下 qwen-plus 无参函数调用会抛 IllegalArgumentException，故暂用 call() 替代 stream()。
     */
    public SseEmitter chat(AgentChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);
        Integer staffId = securityUtil.getCurrentOperatorId();
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        AgentSession session = getOrCreateSession(request, staffId);

        AgentMessage userMsg = new AgentMessage()
                .setSessionId(session.getId()).setRole("user")
                .setContent(request.getMessage()).setTokenCount(estimateTokens(request.getMessage()))
                .setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        String answer;
        try {
            answer = chatClient.prompt()
                    .user(request.getMessage())
                    .tools(assistantTools)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("ChatClient call failed", e);
            answer = "抱歉，AI 服务暂时不可用，请稍后重试。";
        }

        if (answer == null || answer.isBlank()) {
            answer = "抱歉，未能生成回复，请稍后重试。";
        }

        final String finalAnswer = answer;
        final Long sessionId = session.getId();

        new Thread(() -> {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                for (int i = 0; i < finalAnswer.length(); i++) {
                    String ch = finalAnswer.substring(i, i + 1);
                    emitter.send(SseEmitter.event().name("token").data(ch));
                    if (i % 5 == 0) Thread.sleep(5);
                }
                AgentMessage assistantMsg = new AgentMessage()
                        .setSessionId(sessionId).setRole("assistant")
                        .setContent(finalAnswer).setTokenCount(estimateTokens(finalAnswer))
                        .setCreatedAt(LocalDateTime.now());
                messageMapper.insert(assistantMsg);
                memoryService.afterMessage(sessionMapper.selectById(sessionId));
                emitter.send(SseEmitter.event().name("meta")
                        .data(Map.of("conversationId", sessionId, "suggestions",
                                List.of("查询我的考勤", "请假流程是什么", "本月薪资明细"))));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE send failed", e);
                emitter.completeWithError(e);
            } finally {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }).start();

        return emitter;
    }

    private AgentSession getOrCreateSession(AgentChatRequest request, Integer staffId) {
        if (request.getSessionId() != null) {
            AgentSession session = sessionMapper.selectById(request.getSessionId());
            if (session != null) return session;
        }

        AgentSession session = new AgentSession()
                .setStaffId(staffId)
                .setTitle(request.getMessage() != null
                        ? request.getMessage().substring(0, Math.min(50, request.getMessage().length()))
                        : "新对话")
                .setMode(request.getMode() != null ? request.getMode() : "CHAT")
                .setMessageCount(0)
                .setTotalTokens(0L)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    private int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, text.length() / 2);
    }

    /** 获取用户的历史会话列表 */
    public List<AgentSession> listSessions() {
        Integer staffId = securityUtil.getCurrentOperatorId();
        return sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentSession>()
                        .eq("staff_id", staffId).orderByDesc("updated_at"));
    }

    /** 获取单个会话详情 */
    public AgentSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /** 获取会话消息历史 */
    public List<AgentMessage> listMessages(Long sessionId) {
        return messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentMessage>()
                        .eq("session_id", sessionId).orderByAsc("id"));
    }

    private static final Set<String> VALID_MODES = Set.of("CHAT", "KB_SEARCH");

    /** 切换会话模式 */
    public void switchMode(Long sessionId, String mode) {
        if (mode != null && !VALID_MODES.contains(mode)) {
            throw new IllegalArgumentException("不支持的模式: " + mode + "，有效值: CHAT, KB_SEARCH");
        }
        AgentSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setMode(mode);
            sessionMapper.updateById(session);
        }
    }

    /** 删除会话 */
    @Transactional
    public void deleteSession(Long sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteById(sessionId);
    }
}
