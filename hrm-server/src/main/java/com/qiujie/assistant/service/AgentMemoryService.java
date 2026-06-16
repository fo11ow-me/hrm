package com.qiujie.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.common.llm.LlmProvider;
import com.qiujie.assistant.entity.AgentMessage;
import com.qiujie.assistant.entity.AgentSession;
import com.qiujie.assistant.mapper.AgentMessageMapper;
import com.qiujie.assistant.mapper.AgentSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 短期记忆三级压缩服务。
 * L1 会话记忆 → L2 紧凑摘要 → L3 运行时截断
 */
@Service
public class AgentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryService.class);

    @Autowired
    private AgentSessionMapper sessionMapper;

    @Autowired
    private AgentMessageMapper messageMapper;

    @Autowired(required = false)
    private LlmProvider llmClient;

    @Value("${agent.memory.l1-update-interval:5}")
    private int l1UpdateInterval; // 每 N 轮消息更新一次 L1

    @Value("${agent.memory.l2-update-interval:15}")
    private int l2UpdateInterval; // 每 N 轮消息更新一次 L2

    @Value("${agent.memory.max-tokens:50000}")
    private int maxTokens;

    @Value("${agent.memory.keep-recent:5}")
    private int keepRecent;

    /**
     * 在 LLM 调用前注入上下文（BEFORE_MODEL Hook）。
     * 返回 system message 内容。
     */
    public String buildContext(AgentSession session) {
        StringBuilder ctx = new StringBuilder();

        // L2 紧凑摘要
        if (session.getCompactSummary() != null && !session.getCompactSummary().isBlank()) {
            ctx.append("【历史对话精要】\n").append(session.getCompactSummary()).append("\n\n");
        }

        // L1 会话记忆
        if (session.getSessionMemory() != null && !session.getSessionMemory().isBlank()) {
            ctx.append("【当前会话关键信息】\n").append(session.getSessionMemory()).append("\n\n");
        }

        return ctx.toString();
    }

    /**
     * 获取最近消息（L3 运行时截断）。
     */
    public List<AgentMessage> getRecentMessages(Long sessionId) {
        List<AgentMessage> all = messageMapper.selectList(
                new QueryWrapper<AgentMessage>().eq("session_id", sessionId)
                        .orderByAsc("id"));

        // L3 截断：估算 token 数（~2 chars/token），超过则只保留最近 N 条
        long estimatedTokens = all.stream().mapToLong(m ->
                m.getContent() != null ? m.getContent().length() / 2 : 0).sum();
        if (estimatedTokens > maxTokens) {
            log.warn("Session {} token overflow: {} > {}, truncating", sessionId, estimatedTokens, maxTokens);
            return all.subList(Math.max(0, all.size() - keepRecent), all.size());
        }
        return all;
    }

    /**
     * 消息发送后触发记忆更新。
     */
    public void afterMessage(AgentSession session) {
        int count = session.getMessageCount() != null ? session.getMessageCount() + 1 : 1;
        session.setMessageCount(count);
        session.setUpdatedAt(LocalDateTime.now());

        // L1 更新：每 N 轮触发增量摘要
        if (count % l1UpdateInterval == 0) {
            updateSessionMemory(session);
        }

        // L2 更新：每 3*N 轮触发精炼压缩
        if (count % l2UpdateInterval == 0) {
            updateCompactSummary(session);
        }

        sessionMapper.updateById(session);
    }

    private void updateSessionMemory(AgentSession session) {
        if (llmClient == null) return;
        List<AgentMessage> recent = getRecentMessages(session.getId());
        if (recent.size() < l1UpdateInterval) return;

        StringBuilder history = new StringBuilder();
        for (AgentMessage m : recent) {
            history.append(m.getRole()).append(": ").append(m.getContent()).append("\n");
        }

        String existing = session.getSessionMemory() != null ? session.getSessionMemory() : "";
        String prompt = String.format(
                "以下是对话历史。请以要点形式总结新增的关键事实、决策和用户偏好。保留已有记忆中的重要信息。\n\n"
                + "已有记忆：\n%s\n\n新增对话：\n%s\n\n更新后的完整记忆（要点形式，中文，不超过500字）：",
                existing, history.toString());
        try {
            String updated = llmClient.generate(prompt, "");
            if (updated != null && !updated.isBlank()) {
                session.setSessionMemory(updated.trim());
            }
        } catch (Exception e) {
            log.warn("L1 memory update failed: {}", e.getMessage());
        }
    }

    private void updateCompactSummary(AgentSession session) {
        if (llmClient == null) return;
        String memory = session.getSessionMemory();
        if (memory == null || memory.isBlank()) return;

        String existing = session.getCompactSummary() != null ? session.getCompactSummary() : "";
        String prompt = String.format(
                "请将以下会话记忆压缩为一段精炼的摘要（最多300字），保留最重要的信息。\n\n"
                + "已有摘要：%s\n\n会话记忆：%s\n\n精炼摘要：",
                existing, memory);
        try {
            String compressed = llmClient.generate(prompt, "");
            if (compressed != null && !compressed.isBlank()) {
                session.setCompactSummary(compressed.trim());
            }
        } catch (Exception e) {
            log.warn("L2 compact summary failed: {}", e.getMessage());
        }
    }
}
