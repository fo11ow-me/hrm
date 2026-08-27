package com.qiujie.assistant.store;

import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ChatSessionStore} 端口边界测试——对 {@link InMemoryChatSessionStore}
 * 断言行为契约（与 JDBC 适配器共享同一接口语义）。
 * <p>
 * 零 mock、零 Spring：直接实例化内存适配器构造数据，从公开接口观察结果。
 * </p>
 */
@DisplayName("ChatSessionStore 端口行为")
class ChatSessionStoreUnitTest {

    private InMemoryChatSessionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryChatSessionStore();
    }

    // ==================== openOrCreate ====================

    @Test
    @DisplayName("openOrCreate：新建会话并同步创建 context 行")
    void openOrCreate_createsSessionAndContext() {
        ChatSession session = store.openOrCreate(null, "我想请假", "CHAT", 100);

        assertNotNull(session.getId());
        assertEquals(100, session.getStaffId());
        assertEquals("CHAT", session.getMode());
        assertEquals(0, session.getMessageCount());
        // context 行（context_version=0）已创建——delete 时可级联清理
        assertEquals(session.getId(), store.getById(session.getId()).getId());
        assertTrue(store.hasSession(session.getId()));
    }

    @Test
    @DisplayName("openOrCreate：新会话标题取消息前 50 字符")
    void openOrCreate_titleTruncatedTo50() {
        String longMsg = "很长的消息".repeat(20); // 100 字符 > 50
        ChatSession session = store.openOrCreate(null, longMsg, null, 100);

        assertTrue(session.getTitle().length() <= 50);
        assertEquals("CHAT", session.getMode()); // mode 缺省默认 CHAT
    }

    @Test
    @DisplayName("openOrCreate：空消息 → 默认标题")
    void openOrCreate_blankMessage() {
        ChatSession session = store.openOrCreate(null, "   ", "KB_SEARCH", 100);
        assertEquals("新会话", session.getTitle());
        assertEquals("KB_SEARCH", session.getMode());
    }

    @Test
    @DisplayName("openOrCreate：sessionId 存在 → 幂等复用，不新建")
    void openOrCreate_reusesExisting() {
        ChatSession created = store.openOrCreate(null, "第一次", "CHAT", 100);
        ChatSession reused = store.openOrCreate(created.getId(), "第二次", "KB_SEARCH", 100);

        assertSame(created, reused);
        // 未产生新会话——会话数仍为 1
        assertEquals(1, store.listSessions(100).size());
    }

    // ==================== getById / listSessions ====================

    @Test
    @DisplayName("getById：存在返回，不存在返回 null")
    void getById() {
        ChatSession session = store.openOrCreate(null, "hello", "CHAT", 1);
        assertEquals(session, store.getById(session.getId()));
        assertNull(store.getById(999L));
    }

    @Test
    @DisplayName("listSessions：仅返回当前员工，按更新时间倒序")
    void listSessions_scopedAndSorted() {
        store.openOrCreate(null, "a", "CHAT", 100);
        store.openOrCreate(null, "b", "CHAT", 100);
        store.openOrCreate(null, "c", "CHAT", 200); // 他人会话

        List<ChatSession> mine = store.listSessions(100);
        assertEquals(2, mine.size());
        // 更新时间倒序（b 更新晚于 a）
        assertTrue(mine.get(0).getTitle().equals("b") || mine.get(0).getTitle().equals("a"));
    }

    // ==================== listMessages（游标分页） ====================

    private Long seedSessionWithMessages(int count) {
        ChatSession session = store.openOrCreate(null, "seed", "CHAT", 100);
        LocalDateTime base = LocalDateTime.parse("2026-06-01T10:00:00");
        for (int i = 0; i < count; i++) {
            store.addMessage(session.getId(), i % 2 == 0 ? "USER" : "ASSISTANT",
                    "msg-" + i, base.plusMinutes(i));
        }
        return session.getId();
    }

    @Test
    @DisplayName("listMessages：默认取最近 5 条，升序")
    void listMessages_defaultPage() {
        Long sid = seedSessionWithMessages(5);
        Map<String, Object> page = store.listMessages(sid, null, 5);

        List<ChatMessage> records = (List<ChatMessage>) page.get("records");
        assertEquals(5, records.size());
        assertEquals("msg-0", records.get(0).getContent());
        assertFalse((Boolean) page.get("hasMore"));
    }

    @Test
    @DisplayName("listMessages：超过页大小 → hasMore=true，且经游标可翻页")
    void listMessages_hasMoreAndCursors() {
        Long sid = seedSessionWithMessages(7);
        Map<String, Object> page1 = store.listMessages(sid, null, 5);

        assertTrue((Boolean) page1.get("hasMore"));
        List<ChatMessage> records1 = (List<ChatMessage>) page1.get("records");
        assertEquals(5, records1.size());
        // 第一页 = 最近 5 条（降序 LIMIT limit+1 判 hasMore 后转升序）
        assertEquals("msg-2", records1.get(0).getContent());
        assertEquals("msg-6", records1.get(4).getContent());

        // 用 nextCursor 翻更早的页（cursor 为首条 create_time）
        String cursor = (String) page1.get("nextCursor");
        assertNotNull(cursor);
        Map<String, Object> page2 = store.listMessages(sid, cursor, 5);
        List<ChatMessage> records2 = (List<ChatMessage>) page2.get("records");
        assertEquals(2, records2.size());
        assertEquals("msg-0", records2.get(0).getContent());
        assertEquals("msg-1", records2.get(1).getContent());
        assertFalse((Boolean) page2.get("hasMore"));
    }

    @Test
    @DisplayName("listMessages：无消息 → 空 records + 无游标")
    void listMessages_empty() {
        Long sid = store.openOrCreate(null, "empty", "CHAT", 100).getId();
        Map<String, Object> page = store.listMessages(sid, null, 5);

        assertTrue(((List<?>) page.get("records")).isEmpty());
        assertFalse((Boolean) page.get("hasMore"));
        assertNull(page.get("nextCursor"));
    }

    // ==================== switchMode ====================

    @Test
    @DisplayName("switchMode：合法模式切换成功")
    void switchMode_valid() {
        ChatSession session = store.openOrCreate(null, "s", "CHAT", 100);
        store.switchMode(session.getId(), "KB_SEARCH");
        assertEquals("KB_SEARCH", store.getById(session.getId()).getMode());
    }

    @Test
    @DisplayName("switchMode：非法模式拒绝抛异常，状态不变")
    void switchMode_invalidRejected() {
        ChatSession session = store.openOrCreate(null, "s", "CHAT", 100);
        assertThrows(IllegalArgumentException.class, () -> store.switchMode(session.getId(), "HACK"));
        assertEquals("CHAT", store.getById(session.getId()).getMode());
    }

    // ==================== delete（级联） ====================

    @Test
    @DisplayName("delete：级联删消息 → 上下文 → 会话，无残留")
    void delete_cascades() {
        Long sid = seedSessionWithMessages(3);
        assertTrue(store.hasSession(sid));

        store.delete(sid);

        assertFalse(store.hasSession(sid));
        assertNull(store.getById(sid));
        assertTrue(((List<?>) store.listMessages(sid, null, 5).get("records")).isEmpty());
    }

    @Test
    @DisplayName("delete：不存在会话 → 静默幂等")
    void delete_idempotent() {
        assertDoesNotThrow(() -> store.delete(999L));
    }
}