package com.qiujie.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiujie.assistant.entity.AgentMessage;
import com.qiujie.assistant.entity.AgentSession;
import com.qiujie.assistant.entity.AssistantSessionContext;
import com.qiujie.assistant.mapper.AgentMessageMapper;
import com.qiujie.assistant.mapper.AgentSessionMapper;
import com.qiujie.assistant.mapper.AssistantSessionContextMapper;
import com.qiujie.assistant.memory.AssistantMemoryProperties;
import com.qiujie.assistant.memory.AssistantMemorySummarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 助手短期记忆三级压缩服务。
 * L1 会话记忆（每 4 条或 1200 token）→ L2 紧凑摘要（总 token > 6500 且增量达标）→
 * L3 运行时截断（BEFORE_MODEL Hook 或内联检查）。
 * 使用范围追踪增量更新 + 乐观锁并发写入。
 *
 * @author quuj
 */
@Service
public class AssistantMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AssistantMemoryService.class);
    private static final int TOKEN_DIVISOR = 4;

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;
    private final AssistantSessionContextMapper contextMapper;
    private final AssistantMemorySummarizer summarizer;
    private final AssistantMemoryProperties props;

    public AssistantMemoryService(AgentSessionMapper sessionMapper,
            AgentMessageMapper messageMapper,
            AssistantSessionContextMapper contextMapper,
            AssistantMemorySummarizer summarizer,
            AssistantMemoryProperties props) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.contextMapper = contextMapper;
        this.summarizer = summarizer;
        this.props = props;
    }

    /**
     * 组装注入 LLM 的系统上下文：L2 长期记忆 → L1 近期记忆
     *
     * @param session 当前会话
     * @return 系统提示上下文文本
     */
    public String buildContext(AgentSession session) {
        AssistantSessionContext ctx = contextMapper.selectById(session.getId());
        if (ctx == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (ctx.getCompactSummary() != null && !ctx.getCompactSummary().isBlank()) {
            sb.append("【历史对话精要】\n").append(ctx.getCompactSummary()).append("\n\n");
        }
        if (ctx.getSessionMemory() != null && !ctx.getSessionMemory().isBlank()) {
            sb.append("【当前会话关键信息】\n").append(ctx.getSessionMemory()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 消息发送后的记忆更新入口。由 AgentService 在 LLM 调用完成后调用。
     *
     * @param session           当前会话
     * @param toolMode          当前工具模式
     * @param groupId           当前分组 ID（可能为 null）
     * @param currentMessageId  当前轮次最后一条消息 ID
     */
    public void afterMessage(AgentSession session, String toolMode, Long groupId, Long currentMessageId) {
        maintain(session, toolMode, groupId, currentMessageId);
    }

    private void maintain(AgentSession session, String toolMode, Long groupId, Long currentMessageId) {
        List<AgentMessage> allMessages = messageMapper.selectList(
                new QueryWrapper<AgentMessage>()
                        .eq("session_id", session.getId()).orderByAsc("id"));
        if (allMessages.isEmpty()) {
            return;
        }

        AssistantSessionContext ctx = contextMapper.selectById(session.getId());
        long lastRangeEnd = ctx != null && ctx.getSessionMemoryRangeEndMessageId() != null
                ? ctx.getSessionMemoryRangeEndMessageId() : 0L;

        List<AgentMessage> newMessages = allMessages.stream()
                .filter(m -> m.getId() != null && m.getId() > lastRangeEnd)
                .collect(Collectors.toList());

        if (!shouldUpdateSessionMemory(newMessages)) {
            return;
        }

        // 首次写入时插入新行
        if (ctx == null) {
            ctx = newContext(session.getId());
            contextMapper.insert(ctx);
            ctx = contextMapper.selectById(session.getId());
            if (ctx == null) {
                return; // defensive
            }
        }

        AssistantSessionContext toWrite = ctx;
        toWrite.setSessionMemory(summarizer.summarizeSessionMemory(
                ctx.getSessionMemory(), newMessages, toolMode, groupId));
        toWrite.setSessionMemoryBaseMessageId(newMessages.get(0).getId());
        toWrite.setSessionMemoryRangeEndMessageId(newMessages.get(newMessages.size() - 1).getId());
        toWrite.setUpdateTime(LocalDateTime.now());

        int totalTokens = estimateTokens(allMessages);
        int newTokens = estimateTokens(newMessages);
        long expectedVersion = ctx.getContextVersion() != null ? ctx.getContextVersion() : 0L;
        toWrite.setContextVersion(expectedVersion + 1);

        if (shouldCompactSession(totalTokens, newMessages.size(), newTokens)) {
            toWrite.setCompactSummary(summarizer.summarizeCompactSummary(
                    ctx.getCompactSummary(),
                    toWrite.getSessionMemory(),
                    collectMessagesBefore(allMessages, currentMessageId)));
            toWrite.setCompactSummaryBaseMessageId(allMessages.get(0).getId());
            toWrite.setCompactSummaryRangeEndMessageId(
                    newMessages.get(newMessages.size() - 1).getId());
        }

        long writeVersion = expectedVersion;
        updateContext(session.getId(), writeVersion, w -> {
            w.set(AssistantSessionContext::getSessionMemory, toWrite.getSessionMemory());
            w.set(AssistantSessionContext::getSessionMemoryBaseMessageId,
                    toWrite.getSessionMemoryBaseMessageId());
            w.set(AssistantSessionContext::getSessionMemoryRangeEndMessageId,
                    toWrite.getSessionMemoryRangeEndMessageId());
            w.set(AssistantSessionContext::getCompactSummary, toWrite.getCompactSummary());
            w.set(AssistantSessionContext::getCompactSummaryBaseMessageId,
                    toWrite.getCompactSummaryBaseMessageId());
            w.set(AssistantSessionContext::getCompactSummaryRangeEndMessageId,
                    toWrite.getCompactSummaryRangeEndMessageId());
            w.set(AssistantSessionContext::getContextVersion, toWrite.getContextVersion());
            w.set(AssistantSessionContext::getUpdateTime, toWrite.getUpdateTime());
        });

        session.setMessageCount(session.getMessageCount() != null
                ? session.getMessageCount() + 2 : 2);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    private boolean shouldUpdateSessionMemory(List<AgentMessage> newMessages) {
        if (newMessages.isEmpty()) {
            return false;
        }
        int tokens = estimateTokens(newMessages);
        return newMessages.size() >= props.getL1MessageTrigger()
                || tokens >= props.getL1TokenTrigger();
    }

    private boolean shouldCompactSession(int totalTokens, int newMsgCount, int newTokens) {
        return totalTokens > props.getSessionTokenThreshold()
                && (newMsgCount >= props.getL2MessageTrigger()
                || newTokens >= props.getL2TokenTrigger());
    }

    /**
     * 估算消息列表的 token 数（字符数 / 4）
     *
     * @param messages 消息列表
     * @return 估算的 token 数，至少 1
     */
    int estimateTokens(List<AgentMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int totalChars = messages.stream()
                .map(AgentMessage::getContent)
                .filter(c -> c != null && !c.isBlank())
                .mapToInt(String::length)
                .sum();
        return Math.max(1, totalChars / TOKEN_DIVISOR);
    }

    private List<AgentMessage> collectMessagesBefore(
            List<AgentMessage> all, Long currentMessageId) {
        return all.stream()
                .filter(m -> currentMessageId == null || m.getId() == null
                        || m.getId() < currentMessageId)
                .collect(Collectors.toList());
    }

    private AssistantSessionContext newContext(Long sessionId) {
        AssistantSessionContext ctx = new AssistantSessionContext();
        ctx.setSessionId(sessionId);
        ctx.setContextVersion(0L);
        ctx.setUpdateTime(LocalDateTime.now());
        return ctx;
    }

    /**
     * 乐观锁上下文更新：CAS context_version，冲突重试最多 3 次
     *
     * @param sessionId      会话 ID
     * @param expectedVersion 期望的版本号
     * @param updater        更新器消费者
     */
    private void updateContext(Long sessionId, long expectedVersion,
            Consumer<LambdaUpdateWrapper<AssistantSessionContext>> updater) {
        for (int i = 0; i < 3; i++) {
            LambdaUpdateWrapper<AssistantSessionContext> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(AssistantSessionContext::getSessionId, sessionId);
            wrapper.eq(AssistantSessionContext::getContextVersion, expectedVersion);
            updater.accept(wrapper);
            int rows = contextMapper.update(null, wrapper);
            if (rows > 0) {
                return;
            }
            AssistantSessionContext latest = contextMapper.selectById(sessionId);
            if (latest == null) {
                return;
            }
            expectedVersion = latest.getContextVersion();
        }
        log.warn("Context update failed after 3 retries for session {}", sessionId);
    }
}
