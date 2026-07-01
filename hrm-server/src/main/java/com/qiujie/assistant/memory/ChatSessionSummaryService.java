package com.qiujie.assistant.memory;

import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSessionContext;
import com.qiujie.assistant.mapper.ChatSessionContextMapper;
import com.qiujie.assistant.service.ChatMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 前端展示用的非 LLM 会话摘要服务。<br>
 * 基于规则（消息数量 / token 估算 / 时间间隔）判断是否需要摘要，
 * 摘要内容由 text-based 拼接生成，不依赖 LLM 调用。
 *
 * @author qiujie
 */
@Service
public class ChatSessionSummaryService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionSummaryService.class);
    private static final int SUMMARY_CHAR_LIMIT = 2000;
    private static final int PER_MESSAGE_CHAR_LIMIT = 160;
    private final ChatSessionContextMapper contextMapper;
    private final Clock clock;
    private final int messageThreshold;
    private final int tokenThreshold;
    private final int staleDays;

    /**
     * 全参构造（Spring 自动注入）
     *
     * @param contextMapper    会话上下文 Mapper
     * @param messageThreshold 触发摘要的消息数量阈值
     * @param tokenThreshold   触发摘要的 token 估算阈值
     * @param staleDays        摘要过期天数
     */
    public ChatSessionSummaryService(
            ChatSessionContextMapper contextMapper,
            @Value("${chat.session.summary.message-threshold:20}") int messageThreshold,
            @Value("${chat.session.summary.token-threshold:8000}") int tokenThreshold,
            @Value("${chat.session.summary.stale-days:7}") int staleDays) {
        this.contextMapper = contextMapper;
        this.clock = Clock.systemDefaultZone();
        this.messageThreshold = messageThreshold;
        this.tokenThreshold = tokenThreshold;
        this.staleDays = staleDays;
    }

    /**
     * 加载可复用的历史摘要。如果上下文不存在、摘要为空或已过期，返回 {@code null}。
     *
     * @param sessionId     会话 ID
     * @param lastMessageAt 最后一条消息的创建时间
     * @return 摘要文本，或 {@code null}
     */
    public String loadReusableSummary(Long sessionId, LocalDateTime lastMessageAt) {
        ChatSessionContext ctx = contextMapper.selectById(sessionId);
        if (ctx == null || ctx.getSummaryText() == null || ctx.getSummaryText().isBlank()) {
            return null;
        }
        if (isStale(ctx.getUpdateTime(), lastMessageAt)) {
            return null;
        }
        return ctx.getSummaryText();
    }

    /**
     * 判断是否需要对当前会话进行摘要。
     *
     * @param totalMessages  消息总数
     * @param estimatedTokens token 估算值
     * @param lastMessageAt  最后一条消息的时间
     * @return true 表示需要摘要
     */
    public boolean shouldSummarize(long totalMessages, int estimatedTokens, LocalDateTime lastMessageAt) {
        if (totalMessages > messageThreshold || estimatedTokens > tokenThreshold) {
            return true;
        }
        if (lastMessageAt == null) {
            return false;
        }
        return ChronoUnit.DAYS.between(lastMessageAt, LocalDateTime.now(clock)) > staleDays;
    }

    /** 估算消息列表 token 数——统一调用 ChatMemoryService 的静态方法，保持全局一致 */
    public int estimateTokens(List<ChatMessage> messages) {
        return ChatMemoryService.estimateTokens(messages);
    }

    /**
     * 对消息列表进行摘要并持久化。保留最近 {@code recentLimit} 条消息作为上下文，
     * 仅对历史消息生成摘要。
     *
     * @param sessionId   会话 ID
     * @param messages    消息列表
     * @param recentLimit 保留为上下文的最近消息数量
     * @return 摘要文本，无需摘要时返回 {@code null}
     */
    public String summarizeAndPersist(Long sessionId, List<ChatMessage> messages, int recentLimit) {
        if (messages == null || messages.isEmpty()) return null;
        int keepRecent = Math.max(1, recentLimit);
        int summaryCount = Math.max(0, messages.size() - keepRecent);
        if (summaryCount == 0) return null;
        List<ChatMessage> forSummary = messages.subList(0, summaryCount);
        String text = buildSummaryText(forSummary);
        ChatSessionContext ctx = contextMapper.selectById(sessionId);
        if (ctx == null) {
            ctx = new ChatSessionContext();
            ctx.setSessionId(sessionId);
            ctx.setContextVersion(0L);
        }
        ctx.setSummaryText(text);
        ctx.setSourceMessageId(forSummary.get(forSummary.size() - 1).getId());
        ctx.setUpdateTime(LocalDateTime.now(clock));
        contextMapper.updateById(ctx);
        return text;
    }

    /**
     * 判断摘要是否过期。
     *
     * @param updatedAt     摘要更新时间
     * @param lastMessageAt 最后消息时间
     * @return true 表示已过期
     */
    private boolean isStale(LocalDateTime updatedAt, LocalDateTime lastMessageAt) {
        if (updatedAt == null) return true;
        if (lastMessageAt != null && updatedAt.isBefore(lastMessageAt)) return true;
        return ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now(clock)) > staleDays;
    }

    /**
     * 使用规则拼接摘要文本，不使用 LLM。
     *
     * @param messages 消息列表
     * @return 摘要文本
     */
    private String buildSummaryText(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder("历史摘要:").append(System.lineSeparator());
        int currentChars = sb.length();
        for (ChatMessage m : messages) {
            String line = "- %s：%s".formatted(
                    "USER".equals(m.getRole()) ? "用户" : "助手",
                    truncateContent(m.getContent()));
            if (currentChars + line.length() > SUMMARY_CHAR_LIMIT) {
                sb.append("- 其余历史消息已省略").append(System.lineSeparator());
                break;
            }
            sb.append(line).append(System.lineSeparator());
            currentChars = sb.length();
        }
        return sb.toString().trim();
    }

    /**
     * 截断单条消息内容到限制长度。
     *
     * @param content 原始内容
     * @return 截断后的内容
     */
    private String truncateContent(String content) {
        if (content == null) return "";
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= PER_MESSAGE_CHAR_LIMIT
                ? normalized : normalized.substring(0, PER_MESSAGE_CHAR_LIMIT) + "...";
    }
}
