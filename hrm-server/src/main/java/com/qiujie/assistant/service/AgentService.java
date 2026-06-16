package com.qiujie.assistant.service;

import cn.hutool.core.util.IdUtil;
import com.qiujie.assistant.AssistantTools;
import com.qiujie.common.llm.LlmProvider;
import com.qiujie.assistant.dto.AgentChatRequest;
import com.qiujie.assistant.entity.AgentMessage;
import com.qiujie.assistant.entity.AgentSession;
import com.qiujie.assistant.mapper.AgentMessageMapper;
import com.qiujie.assistant.mapper.AgentSessionMapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.spi.KnowledgeSearchProvider;

import com.qiujie.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
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

    @Autowired(required = false)
    private LlmProvider llmClient;

    @Autowired
    private AgentSessionMapper sessionMapper;

    @Autowired
    private AgentMessageMapper messageMapper;

    @Autowired
    private AgentMemoryService memoryService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private KnowledgeSearchProvider retrievalService;

    @Autowired
    private AssistantTools assistantTools;

    private final ChatClient chatClient;

    public AgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


    private static final String SYSTEM_PROMPT = """
            你是 HRM 系统的 AI 智能助手。
            你可以：
            1. 回答人力资源相关问题（考勤、薪资、请假、政策等）
            2. 在知识库检索模式下，检索公司制度、员工手册等文档回答问题
            3. 进行友好的日常对话

            回答要求：
            - 准确、简洁、专业
            - 如果使用了知识库检索，在回答末尾标注信息来源
            - 不确定的信息请明确说明
            """;

    /**
     * 同步对话（前端 AssistantChat.vue 使用）。
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

    @Transactional
    public SseEmitter chat(AgentChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L); // 无超时
        Integer staffId = securityUtil.getCurrentOperatorId();

        // 1. 获取或创建会话
        AgentSession session = getOrCreateSession(request, staffId);

        // 2. 保存用户消息
        AgentMessage userMsg = new AgentMessage()
                .setSessionId(session.getId())
                .setRole("user")
                .setContent(request.getMessage())
                .setTokenCount(estimateTokens(request.getMessage()))
                .setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 3. 异步执行 ReAct 循环
        executeAsync(session, request.getMessage(), request.getMode(), emitter);

        return emitter;
    }

    @Async
    private void executeAsync(AgentSession session, String message, String mode, SseEmitter emitter) {
        try {
            AgentMessage assistantMsg = reactLoop(session, message, mode, emitter);
            if (assistantMsg != null) {
                messageMapper.insert(assistantMsg);
                memoryService.afterMessage(session);
            }
            emitter.complete();
        } catch (Exception e) {
            log.error("Agent loop failed", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("处理请求时出错"));
            } catch (IOException ignored) {}
            emitter.completeWithError(e);
        }
    }

    private AgentMessage reactLoop(AgentSession session, String userInput, String hintMode, SseEmitter emitter) throws IOException {
        String mode = resolveMode(session, hintMode);

        // Step 1: BEFORE_MODEL — 注入上下文
        String context = memoryService.buildContext(session);
        List<AgentMessage> history = memoryService.getRecentMessages(session.getId());

        // Step 2: 知识库检索（KB_SEARCH 模式）
        String kbContext = "";
        if ("KB_SEARCH".equals(mode)) {
            kbContext = searchKnowledgeBase(userInput);
            if (kbContext != null && !kbContext.isBlank()) {
                emitter.send(SseEmitter.event().name("status").data("已检索知识库"));
            }
        }

        // Step 3: 构建 prompt 并调用 LLM
        String prompt = buildPrompt(mode, context, kbContext, history, userInput);
        String fullResponse = callLlm(prompt);

        // Step 4: SSE 流式输出（Delta 去重）
        streamResponse(fullResponse, emitter);

        // Step 5: 构建助手消息
        AgentMessage msg = new AgentMessage()
                .setSessionId(session.getId())
                .setRole("assistant")
                .setContent(fullResponse)
                .setTokenCount(estimateTokens(fullResponse))
                .setCreatedAt(LocalDateTime.now());

        return msg;
    }

    private String resolveMode(AgentSession session, String hintMode) {
        if (hintMode != null && !hintMode.isBlank()) {
            if (!hintMode.equals(session.getMode())) {
                session.setMode(hintMode);
                sessionMapper.updateById(session);
            }
            return hintMode;
        }
        return session.getMode() != null ? session.getMode() : "CHAT";
    }

    private String buildPrompt(String mode, String context, String kbContext,
                                List<AgentMessage> history, String userInput) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT);

        if (!context.isBlank()) {
            prompt.append("\n").append(context);
        }

        if (!kbContext.isBlank()) {
            prompt.append("\n【知识库参考资料】\n").append(kbContext);
            prompt.append("\n请基于上述参考资料回答用户问题，并注明信息来源。\n");
        }

        prompt.append("\n当前模式：").append(mode);

        // 注入最近消息
        for (AgentMessage m : history) {
            String roleLabel = switch (m.getRole()) {
                case "user" -> "用户";
                case "assistant" -> "助手";
                default -> m.getRole();
            };
            prompt.append("\n").append(roleLabel).append("：").append(m.getContent());
        }
        prompt.append("\n用户：").append(userInput).append("\n助手：");

        return prompt.toString();
    }

    private String searchKnowledgeBase(String query) {
        try {
            var results = retrievalService.search(List.of(query));
            if (results.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(results.size(), 5); i++) {
                var r = results.get(i);
                sb.append(String.format("[%d] 《%s》: %s\n",
                        i + 1, r.documentName(), r.chunkText()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("KB search in agent failed: {}", e.getMessage());
            return "";
        }
    }

    private String callLlm(String prompt) {
        if (llmClient == null) return "AI 助手未配置，请联系管理员。";
        try {
            return llmClient.generate(prompt, "");
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            return "抱歉，AI 服务暂时不可用，请稍后重试。";
        }
    }

    private void streamResponse(String fullResponse, SseEmitter emitter) throws IOException {
        Set<String> sent = new HashSet<>();
        for (int i = 0; i < fullResponse.length(); i += 3) {
            String chunk = fullResponse.substring(i, Math.min(i + 3, fullResponse.length()));
            if (sent.add(chunk)) { // Delta 去重
                emitter.send(SseEmitter.event().name("token").data(chunk));
            }
        }
        // AGENT_MODEL_FINISHED 兜底
        emitter.send(SseEmitter.event().name("status").data("AGENT_MODEL_FINISHED"));
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

    /** 获取会话消息历史 */
    public List<AgentMessage> listMessages(Long sessionId) {
        return messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentMessage>()
                        .eq("session_id", sessionId).orderByAsc("id"));
    }

    /** 切换会话模式 */
    public void switchMode(Long sessionId, String mode) {
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
