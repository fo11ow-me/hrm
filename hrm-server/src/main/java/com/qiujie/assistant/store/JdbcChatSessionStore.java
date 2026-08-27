package com.qiujie.assistant.store;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.entity.ChatSessionContext;
import com.qiujie.assistant.mapper.ChatMessageMapper;
import com.qiujie.assistant.mapper.ChatSessionContextMapper;
import com.qiujie.assistant.mapper.ChatSessionMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会话存储 JDBC 适配器——{@link ChatSessionStore} 的生产实现。
 * <p>
 * 包 3 个 Mapper，隐藏表结构、索引策略与级联删除顺序。
 * 会话/消息/上下文行的生命周期编排全部收口于此，调用方不接触 MyBatis-Plus。
 * </p>
 */
@Component
public class JdbcChatSessionStore implements ChatSessionStore {

    /** 单次最多返回消息条数，防恶意请求 */
    private static final int MAX_PAGE_SIZE = 50;

    /** 合法模式白名单，防注入 */
    private static final Set<String> VALID_MODES = Set.of("CHAT", "KB_SEARCH");

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionContextMapper contextMapper;

    public JdbcChatSessionStore(ChatSessionMapper sessionMapper,
                                ChatMessageMapper messageMapper,
                                ChatSessionContextMapper contextMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.contextMapper = contextMapper;
    }

    @Override
    public ChatSession openOrCreate(Long sessionId, String message, String mode, Integer staffId) {
        // 分支 1：复用已有会话
        if (sessionId != null) {
            ChatSession session = sessionMapper.selectById(sessionId);
            if (session != null) return session;
        }

        // 分支 2：新建会话
        ChatSession session = new ChatSession()
                .setStaffId(staffId)
                .setTitle(message != null && !message.isBlank()
                        ? message.substring(0, Math.min(50, message.length()))
                        : "新会话")
                .setMode(mode != null ? mode : "CHAT")
                .setMessageCount(0)
                .setTotalTokens(0L)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);

        // 一对一 context 行，session_id 即主键
        ChatSessionContext ctx = new ChatSessionContext();
        ctx.setSessionId(session.getId());
        ctx.setContextVersion(0L);
        ctx.setUpdateTime(LocalDateTime.now());
        contextMapper.insert(ctx);

        return session;
    }

    @Override
    public ChatSession getById(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    @Override
    public List<ChatSession> listSessions(Integer staffId) {
        return sessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("staff_id", staffId)
                        .orderByDesc("update_time"));
    }

    @Override
    public Map<String, Object> listMessages(Long sessionId, String before, int size) {
        int limit = Math.min(size, MAX_PAGE_SIZE);
        var qw = new QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId);
        if (before != null) {
            qw.lt("create_time", before);
        }
        qw.orderByDesc("create_time").last("LIMIT " + (limit + 1));

        List<ChatMessage> desc = messageMapper.selectList(qw);
        boolean hasMore = desc.size() > limit;
        if (hasMore) desc = desc.subList(0, limit);
        Collections.reverse(desc);

        String nextCursor = null;
        if (!desc.isEmpty() && desc.get(0).getCreateTime() != null) {
            nextCursor = desc.get(0).getCreateTime().toString();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", desc);
        result.put("hasMore", hasMore);
        result.put("nextCursor", nextCursor);
        return result;
    }

    @Override
    public void switchMode(Long sessionId, String mode) {
        if (mode != null && !VALID_MODES.contains(mode)) {
            throw new IllegalArgumentException("不支持的模式: " + mode + "，有效值: CHAT, KB_SEARCH");
        }
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setMode(mode);
            sessionMapper.updateById(session);
        }
    }

    @Override
    @Transactional
    public void delete(Long sessionId) {
        // 删除顺序：消息 → 上下文 → 会话（应用层保证顺序，表无 FK）
        messageMapper.deleteBySessionId(sessionId);
        contextMapper.deleteById(sessionId);
        sessionMapper.deleteById(sessionId);
    }
}
