package com.qiujie.assistant.store;

import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.entity.ChatSessionContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会话存储内存适配器——{@link ChatSessionStore} 的测试替身。
 * <p>
 * 用内存 Map 复刻生产行为契约：openOrCreate 幂等复用/新建+context 行、
 * 自增主键、游标分页（降序取 limit+1 判 hasMore）、mode 白名单、级联删除顺序。
 * 零 mock、零 Spring，直接构造即可。
 * </p>
 */
public class InMemoryChatSessionStore implements ChatSessionStore {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> VALID_MODES = Set.of("CHAT", "KB_SEARCH");

    private final Map<Long, ChatSession> sessions = new LinkedHashMap<>();
    private final Map<Long, List<ChatMessage>> messagesBySession = new HashMap<>();
    private final Map<Long, ChatSessionContext> contexts = new HashMap<>();

    private long nextSessionId = 1;
    private long nextMessageId = 1;

    @Override
    public ChatSession openOrCreate(Long sessionId, String message, String mode, Integer staffId) {
        if (sessionId != null) {
            ChatSession existing = sessions.get(sessionId);
            if (existing != null) return existing;
        }

        ChatSession session = new ChatSession()
                .setId(nextSessionId++)
                .setStaffId(staffId)
                .setTitle(message != null && !message.isBlank()
                        ? message.substring(0, Math.min(50, message.length()))
                        : "新会话")
                .setMode(mode != null ? mode : "CHAT")
                .setMessageCount(0)
                .setTotalTokens(0L)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sessions.put(session.getId(), session);
        messagesBySession.put(session.getId(), new ArrayList<>());

        ChatSessionContext ctx = new ChatSessionContext();
        ctx.setSessionId(session.getId());
        ctx.setContextVersion(0L);
        ctx.setUpdateTime(LocalDateTime.now());
        contexts.put(session.getId(), ctx);

        return session;
    }

    @Override
    public ChatSession getById(Long sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public List<ChatSession> listSessions(Integer staffId) {
        List<ChatSession> result = new ArrayList<>();
        for (ChatSession s : sessions.values()) {
            if (s.getStaffId() != null && s.getStaffId().equals(staffId)) {
                result.add(s);
            }
        }
        result.sort(Comparator.comparing(ChatSession::getUpdateTime).reversed());
        return result;
    }

    @Override
    public Map<String, Object> listMessages(Long sessionId, String before, int size) {
        int limit = Math.min(size, MAX_PAGE_SIZE);
        List<ChatMessage> all = messagesBySession.getOrDefault(sessionId, new ArrayList<>());

        // 过滤 before 游标（create_time < before）
        List<ChatMessage> filtered = new ArrayList<>();
        for (ChatMessage m : all) {
            if (before != null && m.getCreateTime() != null
                    && !m.getCreateTime().isBefore(LocalDateTime.parse(before))) {
                continue;
            }
            filtered.add(m);
        }

        // 降序取 limit+1 判 hasMore
        List<ChatMessage> desc = new ArrayList<>(filtered);
        desc.sort(Comparator.comparing(ChatMessage::getCreateTime).reversed());
        boolean hasMore = desc.size() > limit;
        if (hasMore) desc = desc.subList(0, limit);

        // 转升序
        List<ChatMessage> records = new ArrayList<>(desc);
        records.sort(Comparator.comparing(ChatMessage::getCreateTime));

        String nextCursor = null;
        if (!records.isEmpty() && records.get(0).getCreateTime() != null) {
            nextCursor = records.get(0).getCreateTime().toString();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("hasMore", hasMore);
        result.put("nextCursor", nextCursor);
        return result;
    }

    @Override
    public void switchMode(Long sessionId, String mode) {
        if (mode != null && !VALID_MODES.contains(mode)) {
            throw new IllegalArgumentException("不支持的模式: " + mode + "，有效值: CHAT, KB_SEARCH");
        }
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            session.setMode(mode);
            session.setUpdateTime(LocalDateTime.now());
        }
    }

    @Override
    public void delete(Long sessionId) {
        // 级联删除：消息 → 上下文 → 会话
        messagesBySession.remove(sessionId);
        contexts.remove(sessionId);
        sessions.remove(sessionId);
    }

    // ==================== 测试辅助 ====================

    /** 向指定会话追加一条消息（供分页测试构造数据）。 */
    public ChatMessage addMessage(Long sessionId, String role, String content, LocalDateTime createTime) {
        ChatMessage msg = new ChatMessage()
                .setId(nextMessageId++)
                .setSessionId(sessionId)
                .setRole(role)
                .setContent(content)
                .setCreateTime(createTime);
        messagesBySession.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(msg);
        return msg;
    }

    /** 校验级联删除后无残留。 */
    public boolean hasSession(Long sessionId) {
        return sessions.containsKey(sessionId);
    }
}
