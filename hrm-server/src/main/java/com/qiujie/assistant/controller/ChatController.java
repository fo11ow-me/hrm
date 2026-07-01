package com.qiujie.assistant.controller;

import com.qiujie.assistant.dto.ChatRequest;
import com.qiujie.assistant.entity.ChatMessage;
import com.qiujie.assistant.entity.ChatSession;
import com.qiujie.assistant.service.ChatService;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI 助手控制器：提供同步对话、SSE 流式对话和会话管理三大类接口。
 * <p>
 * 所有业务逻辑都委托给 {@link ChatService}，Controller 仅做路由分发——
 * 这是 Spring MVC 的标准分层实践：Controller 负责 HTTP 协议适配，
 * Service 负责业务编排，两者之间通过 DTO 传递数据。
 * </p>
 *
 * @author quuj
 */
@RestController // 等同于 @Controller + @ResponseBody，所有方法返回值自动序列化为 JSON
@RequestMapping("/assistant") // 统一资源前缀，所有接口在 /assistant 路径下
public class ChatController {

    @Autowired // 按类型注入 AssistantService Bean，负责对话编排、会话持久化、记忆管理等所有业务逻辑
    private ChatService chatService;

    /**
     * 同步对话接口。
     * <p>
     * 前端发送用户消息，后端同步等待 LLM 完整回答后一次性返回。
     * 适用于不需要打字机效果的场景（当前前端 AssistantChat.vue 实际走的就是这个接口）。
     * </p>
     *
     * @param request 包含 conversationId（可选）、message（必填）、mode（可选，默认 CHAT）
     * @return 统一响应体，data 中包含 conversationId、answer、suggestions
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    /**
     * SSE 流式对话接口。
     * <p>
     * 与 {@link #chat(ChatRequest)} 的区别：回答以 SSE（Server-Sent Events）
     * 流式推送给前端，每个字符作为一个 event:token 事件发送，产生打字机效果。
     * 前端使用 EventSource 或 fetch + ReadableStream 消费。
     * </p>
     *
     * @param request 与同步接口结构相同
     * @return SseEmitter 实例，Spring MVC 会将其管理为长连接异步响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    /**
     * 获取当前员工的所有历史会话列表。
     * <p>
     * 按更新时间倒序排列，前端据此渲染左侧会话下拉选择器。
     * 权限范围限定为当前登录员工，由 Service 层通过 SecurityUtil 获取 staffId 控制。
     * </p>
     */
    @GetMapping("/conversations") // GET /assistant/conversations —— 查询资源集合
    public ResponseDTO listSessions() {
        List<ChatSession> sessions = chatService.listSessions(); // 从 ast_chat_session 表查询当前员工的会话
        return Response.success(sessions); // 包装为统一响应格式 { code:200, data:[...] }
    }

    /**
     * 获取单个会话的元数据。
     * <p>
     * 返回会话标题、模式、状态等信息，不含消息列表。
     * 消息列表通过独立的 {@link #listMessages(Long)} 接口分页获取。
     * </p>
     */
    @GetMapping("/conversations/{id}") // GET /assistant/conversations/{id} —— 路径变量占位单个资源
    public ResponseDTO getSession(@PathVariable Long id) { // @PathVariable 从 URL 路径提取 id
        ChatSession session = chatService.getSession(id); // 按主键查询
        if (session == null) {
            return com.qiujie.dto.Response.error("会话不存在"); // 返回 200+error code，前端根据 code 判断
        }
        return Response.success(session);
    }

    /**
     * 分页获取会话的历史消息。
     * <p>
     * 前端打开历史会话时调用，一次加载最近 20 条，
     * 上滑时通过 before 游标加载更早的消息，实现向前翻页。
     * </p>
     */
    @GetMapping("/conversations/{id}/messages") // GET /assistant/conversations/{id}/messages —— 子资源查询
    public ResponseDTO listMessages(@PathVariable Long id) {
        List<ChatMessage> messages = chatService.listMessages(id); // 按 session_id 查询并按 id 升序排列
        return Response.success(messages);
    }

    /**
     * 切换会话模式（CHAT ↔ KB_SEARCH）。
     * <p>
     * CHAT 模式下 LLM 可以调用 @Tool 查询员工个人数据（考勤、请假等）；
     * KB_SEARCH 模式下 LLM 以知识库检索为主。
     * 同一会话内可动态切换，模式记录在 ast_chat_session.mode 字段中。
     * </p>
     */
    @PutMapping("/conversations/{id}/mode") // PUT 语义：替换指定资源的指定属性
    public ResponseDTO switchMode(@PathVariable Long id, @RequestBody Map<String, String> body) { // PUT 请求体中的 mode 值
        chatService.switchMode(id, body.get("mode")); // 校验 mode 值后更新
        return Response.success(); // 无 data 返回，仅表示操作成功
    }

    /**
     * 删除会话。
     * <p>
     * 级联删除会话下的所有消息和上下文记录（在 Service 层的同一个事务中执行），
     * 避免产生孤儿数据。
     * </p>
     */
    @DeleteMapping("/conversations/{id}") // DELETE /assistant/conversations/{id} —— 删除指定资源
    public ResponseDTO deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id); // 事务内依次删除 ast_chat_message → ast_chat_session_context → ast_chat_session
        return Response.success();
    }
}
