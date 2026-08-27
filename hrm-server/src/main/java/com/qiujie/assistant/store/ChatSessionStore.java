package com.qiujie.assistant.store;

import com.qiujie.assistant.entity.ChatSession;

import java.util.List;
import java.util.Map;

/**
 * 会话存储端口——隐藏 {@code ast_chat_session} / {@code ast_chat_message} /
 * {@code ast_chat_session_context} 三张表的全部领域编排。
 * <p>
 * 生产用 {@link JdbcChatSessionStore}（JDBC 适配器），测试用内存适配器。
 * 端口不依赖 {@code SecurityUtil} 或 web 上下文——staffId 由调用方显式传入，
 * 因此可被内存适配器零 mock 直接测试。
 * </p>
 */
public interface ChatSessionStore {

    /**
     * 获取或创建会话——会话存储内唯一的会话/上下文创建点。
     * <p>
     * sessionId 非空且存在 → 复用；否则 → 新建会话并同步创建一对一 context 行
     * （{@code context_version=0}），新会话标题取消息前 50 字符。
     * </p>
     *
     * @return 已存在或新建的会话对象（含自增主键）
     */
    ChatSession openOrCreate(Long sessionId, String message, String mode, Integer staffId);

    /** 按主键获取单个会话元数据（不做所有权校验）。 */
    ChatSession getById(Long sessionId);

    /** 当前员工的历史会话列表，按更新时间倒序。 */
    List<ChatSession> listSessions(Integer staffId);

    /**
     * 游标分页消息历史。
     *
     * @return { records(升序), hasMore, nextCursor }
     */
    Map<String, Object> listMessages(Long sessionId, String before, int size);

    /** 切换会话模式（CHAT ↔ KB_SEARCH），非法模式拒绝。 */
    void switchMode(Long sessionId, String mode);

    /** 删除会话——级联清理（消息 → 上下文 → 会话）。 */
    void delete(Long sessionId);
}
