package com.qiujie.assistant.service;

import com.qiujie.assistant.dto.AgentChatRequest;
import com.qiujie.assistant.entity.AgentMessage;
import com.qiujie.assistant.entity.AgentSession;
import com.qiujie.assistant.entity.AssistantSessionContext;
import com.qiujie.assistant.mapper.AgentMessageMapper;
import com.qiujie.assistant.mapper.AgentSessionMapper;
import com.qiujie.assistant.mapper.AssistantSessionContextMapper;
import com.qiujie.assistant.memory.AssistantMemoryProperties;
import com.qiujie.assistant.memory.AssistantSessionSummaryService;
import com.qiujie.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;
    private final AssistantSessionContextMapper contextMapper;
    private final AssistantMemoryService memoryService;
    private final AssistantSessionSummaryService summaryService;
    private final SecurityUtil securityUtil;
    private final AssistantMemoryProperties memoryProps;
    private final ChatClient chatClient;

    public AgentService(AgentSessionMapper sessionMapper,
            AgentMessageMapper messageMapper,
            AssistantSessionContextMapper contextMapper,
            AssistantMemoryService memoryService,
            AssistantSessionSummaryService summaryService,
            SecurityUtil securityUtil,
            AssistantMemoryProperties memoryProps,
            ChatClient.Builder chatClientBuilder) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.contextMapper = contextMapper;
        this.memoryService = memoryService;
        this.summaryService = summaryService;
        this.securityUtil = securityUtil;
        this.memoryProps = memoryProps;
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional
    public SseEmitter chat(AgentChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);
        Integer staffId = securityUtil.getCurrentOperatorId();
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        AgentSession session = getOrCreateSession(request, staffId);

        AgentMessage userMsg = new AgentMessage()
                .setSessionId(session.getId()).setRole("USER")
                .setContent(request.getMessage())
                .setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 组装 L1/L2 记忆上下文注入 system prompt
        String memoryContext = memoryService.buildContext(session);

        // L3: 加载最近消息，token 超阈值则截断
        List<AgentMessage> recentMessages = loadRecentMessages(session.getId());
        int estimatedTokens = memoryService.estimateTokens(recentMessages);
        if (estimatedTokens > memoryProps.getMaxTokens()) {
            int keep = memoryProps.getKeepRecent();
            recentMessages = recentMessages.subList(
                    Math.max(0, recentMessages.size() - keep), recentMessages.size());
            log.warn("Session {} L3 truncation: {} > {} tokens, kept {} messages",
                    session.getId(), estimatedTokens, memoryProps.getMaxTokens(), keep);
        }

        String answer;
        try {
            // 将截断后的历史消息转换为 Spring AI Message 列表
            List<Message> historyMessages = new ArrayList<>();
            for (AgentMessage m : recentMessages) {
                if (m.getContent() == null || m.getContent().isBlank()) continue;
                if ("USER".equals(m.getRole())) {
                    historyMessages.add(new UserMessage(m.getContent()));
                } else if ("ASSISTANT".equals(m.getRole())) {
                    historyMessages.add(new AssistantMessage(m.getContent()));
                }
            }

            answer = chatClient.prompt()
                    .system(s -> s.text(memoryContext))
                    .messages(historyMessages)
                    .user(request.getMessage())
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM call failed", e);
            answer = "抱歉，AI 服务暂时不可用，请稍后重试。";
        }

        if (answer == null || answer.isBlank()) {
            answer = "抱歉，未能生成回复，请稍后重试。";
        }

        final String finalAnswer = answer;
        final Long sessionId = session.getId();
        final String toolMode = session.getMode();

        new Thread(() -> {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                for (int i = 0; i < finalAnswer.length(); i++) {
                    String ch = finalAnswer.substring(i, i + 1);
                    emitter.send(SseEmitter.event().name("token").data(ch));
                    if (i % 5 == 0) Thread.sleep(5);
                }

                AgentMessage assistantMsg = new AgentMessage()
                        .setSessionId(sessionId).setRole("ASSISTANT")
                        .setContent(finalAnswer)
                        .setCreatedAt(LocalDateTime.now());
                messageMapper.insert(assistantMsg);

                AgentSession s = sessionMapper.selectById(sessionId);
                if (s != null) {
                    s.setMessageCount(s.getMessageCount() + 2);
                    sessionMapper.updateById(s);
                }

                // 触发 L1/L2 记忆更新
                memoryService.afterMessage(
                        sessionMapper.selectById(sessionId), toolMode, null, assistantMsg.getId());

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

    /**
     * 加载指定会话的全部消息（按 id 升序），用于 L3 运行时截断估算。
     */
    private List<AgentMessage> loadRecentMessages(Long sessionId) {
        return messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentMessage>()
                        .eq("session_id", sessionId).orderByAsc("id"));
    }

    /**
     * 获取或创建会话。首次创建时同时初始化 AssistantSessionContext 行。
     */
    private AgentSession getOrCreateSession(AgentChatRequest request, Integer staffId) {
        if (request.getSessionId() != null) {
            AgentSession session = sessionMapper.selectById(request.getSessionId());
            if (session != null) return session;
        }
        AgentSession session = new AgentSession()
                .setStaffId(staffId)
                .setTitle(request.getMessage() != null
                        ? request.getMessage().substring(0, Math.min(50, request.getMessage().length()))
                        : "新会话")
                .setMode(request.getMode() != null ? request.getMode() : "CHAT")
                .setMessageCount(0).setTotalTokens(0L)
                .setCreatedAt(LocalDateTime.now()).setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        // 初始化上下文行，供记忆服务使用
        AssistantSessionContext ctx = new AssistantSessionContext();
        ctx.setSessionId(session.getId());
        ctx.setContextVersion(0L);
        ctx.setUpdateTime(LocalDateTime.now());
        contextMapper.insert(ctx);
        return session;
    }

    /** 获取用户的历史会话列表 */
    public List<AgentSession> listSessions() {
        Integer staffId = securityUtil.getCurrentOperatorId();
        return sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentSession>()
                        .eq("staff_id", staffId).orderByDesc("update_time"));
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

    /** 删除会话及关联消息、上下文 */
    @Transactional
    public void deleteSession(Long sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        contextMapper.deleteById(sessionId);
        sessionMapper.deleteById(sessionId);
    }

    /**
     * 加载前端展示用会话摘要（非 LLM 摘要，用于会话列表页展示）
     *
     * @param sessionId     会话 ID
     * @param lastMessageAt 最后一条消息时间
     * @return 摘要文本，无可复用摘要时返回 null
     */
    public String loadSummaryText(Long sessionId, LocalDateTime lastMessageAt) {
        return summaryService.loadReusableSummary(sessionId, lastMessageAt);
    }
}
