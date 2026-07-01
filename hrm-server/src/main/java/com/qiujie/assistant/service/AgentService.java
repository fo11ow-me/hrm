package com.qiujie.assistant.service;

import com.qiujie.assistant.AssistantTools;
import com.qiujie.assistant.dto.AgentChatRequest;
import com.qiujie.assistant.entity.AssistantMessage;
import com.qiujie.assistant.entity.AssistantSession;
import com.qiujie.assistant.entity.AssistantSessionContext;
import com.qiujie.assistant.mapper.AssistantMessageMapper;
import com.qiujie.assistant.mapper.AssistantSessionContextMapper;
import com.qiujie.assistant.mapper.AssistantSessionMapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 助手核心编排引擎。
 * <p>
 * 职责：管理对话生命周期（会话创建→消息持久化→LLM 调用→记忆更新→响应组装）。
 * LLM 交互采用 Spring AI 的 {@link ChatClient}，工具集通过 {@link AssistantTools} 注入，
 * 运行时会触发 ReAct 循环（思考→工具调用→生成回答）。
 * </p>
 *
 * <h3>两条对话路径</h3>
 * <ul>
 *   <li>{@link #chatSync(AgentChatRequest)} —— 同步，等待 LLM 完整回答后返回 JSON</li>
 *   <li>{@link #chat(AgentChatRequest)} —— SSE 流式，逐字符推送打字机效果</li>
 * </ul>
 *
 * <h3>记忆更新</h3>
 * 每次对话后触发 {@link AssistantMemoryService#afterMessage(AssistantSession)}，
 * 内部按消息数量阈值驱动 L1/L2 压缩，非每条消息都调用 LLM。
 *
 * @author quuj
 */
@Service // 声明为 Spring Bean，默认单例，由 Controller 通过 @Autowired 注入
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class); // 日志门面，实际输出到 Logback

    @Autowired
    private AssistantSessionMapper sessionMapper; // MyBatis-Plus BaseMapper，操作 assistant_session 表

    @Autowired
    private AssistantMessageMapper messageMapper; // 操作 assistant_message 表，含 deleteBySessionId() 自定义方法

    @Autowired
    private AssistantSessionContextMapper contextMapper; // 操作 assistant_session_context 表（记忆和摘要）

    @Autowired
    private AssistantMemoryService memoryService; // 三级记忆压缩服务，在 LLM 调用后触发

    @Autowired
    private SecurityUtil securityUtil; // 从当前 SecurityContext 提取已认证员工的 staffId

    @Autowired
    private AssistantTools assistantTools; // @Tool 注解的方法集合，作为 ChatClient 的可调用工具注入

    private final ChatClient chatClient; // Spring AI 提供的 LLM 客户端，通过 Builder 模式构建后不可变

    /**
     * 构造注入 ChatClient。
     * <p>
     * Spring Boot 自动配置会创建 ChatClient.Builder Bean（根据 application.yml 中
     * spring.ai.openai.* 的配置指向 DashScope 兼容端点），这里调用 .build() 固化配置。
     * 构造注入优于字段注入：chatClient 明确不可变，且方便单元测试 mock。
     * </p>
     */
    public AgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build(); // build() 后 ChatClient 线程安全，可复用
    }

    /**
     * 同步对话：接收用户消息 → 调用 LLM → 返回完整回答。
     * <p>
     * 整个流程在一个数据库事务中完成——用户消息写入、LLM 调用、助手回答写入、
     * 会话统计更新，任一环节失败则全部回滚。
     * </p>
     *
     * @param request 前端 JSON 反序列化结果，含 sessionId(可选)/message(必填)/mode(可选)
     * @return 统一响应，data.conversationId 供前端后续请求关联会话
     */
    @Transactional // 声明式事务：方法内所有 DB 操作在同一事务中，RuntimeException 时回滚
    public ResponseDTO chatSync(AgentChatRequest request) {
        Integer staffId = securityUtil.getCurrentOperatorId(); // 从 JWT Token 中提取当前员工 ID，保证数据隔离
        AssistantSession session = getOrCreateSession(request, staffId); // 复用已有会话或新建，核心分流逻辑

        // —— 持久化用户消息 ——
        // 先于 LLM 调用写入，确保即使 LLM 超时用户消息也不丢失
        AssistantMessage userMsg = new AssistantMessage()
                .setSessionId(session.getId()).setRole("USER") // role 区分 USER / ASSISTANT / TOOL
                .setToolMode(session.getMode()) // 记录消息产生时的模式，后续回看时知道这条消息是在什么模式下产生的
                .setContent(request.getMessage()) // 原文存储，用于记忆压缩和回显
                .setCreateTime(LocalDateTime.now()); // 精确到毫秒，用于按时间排序
        messageMapper.insert(userMsg); // MyBatis-Plus 自动生成 INSERT SQL

        // —— 构建记忆上下文 ——
        // 从 L1/L2 压缩记忆中提取当前会话的关键信息，注入到 system prompt 中，
        // 让 LLM 在回答时"记住"之前对话中的重要事实和用户偏好
        String memoryContext = memoryService.buildContext(session);

        // —— 调用 LLM ——
        // .tools(assistantTools) 是将 @Tool 方法注册到 ChatClient，
        // Spring AI 框架会自动将工具定义转为 OpenAI function calling 格式发送给 LLM。
        // LLM 决策需要调工具时，框架拦截 tool_calls 响应，反射调用对应方法，将结果塞回对话上下文，
        // 这个循环（ReAct）对业务代码透明——我们只需 .call().content() 拿最终文本。
        String answer = chatClient.prompt() // 开始构建 Prompt
                .system(s -> s.text(memoryContext)) // 注入压缩记忆作为系统上下文，LLM 将其视为已知背景
                .user(request.getMessage()) // 用户消息作为 user role 发送
                .tools(assistantTools) // 注册可调用工具集，触发 function calling 能力
                .call() // 同步阻塞调用，内部可能经历多次 LLM ↔ Tool 往返
                .content(); // 提取最终回答的文本内容（不含工具调用的中间 JSON）

        // —— 持久化助手回答 ——
        AssistantMessage assistantMsg = new AssistantMessage()
                .setSessionId(session.getId()).setRole("ASSISTANT") // 助手角色
                .setToolMode(session.getMode())
                .setContent(answer) // LLM 返回的完整文本
                .setCreateTime(LocalDateTime.now());
        messageMapper.insert(assistantMsg);

        // —— 更新会话统计 ——
        // messageCount 记录总消息数，用于记忆更新的阈值判断（每 5/15 条触发压缩）
        session.setMessageCount(session.getMessageCount() + 2); // +2 = USER + ASSISTANT 各一条
        session.setLastMessageAt(LocalDateTime.now()); // 最后活跃时间，用于会话列表排序
        sessionMapper.updateById(session); // 按主键更新，只改动两个字段

        memoryService.afterMessage(session); // 以新 messageCount 触发记忆评估——不一定每次都压缩

        // —— 组装响应 ——
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", session.getId()); // 新会话时前端用此 ID 关联后续请求
        data.put("answer", answer); // LLM 完整回答文本
        data.put("suggestions", List.of("查询我的考勤", "请假流程是什么", "本月薪资明细")); // 前端渲染为快捷追问按钮
        return Response.success(data); // Response.success() 自动设置 code=200
    }

    /**
     * SSE 流式对话：与同步对话共享同一 LLM 调用逻辑，但以 Server-Sent Events 逐字符推送。
     * <p>
     * 架构要点：LLM 调用本身仍是同步的（qwen-plus 不支持 true streaming 或兼容模式下不可用），
     * 拿到完整回答后，在独立线程中模拟逐字推送——每发一个字符 sleep 5ms。
     * 这样前端获得打字机体验，但不节省首字延迟。
     * </p>
     * <p>
     * 线程中的 SecurityContext 需要手动传递：SseEmitter 的发送线程不是请求线程，
     * Spring Security 上下文不会自动继承，所以用 final 变量捕获 Authentication 后手动设置。
     * </p>
     *
     * @param request 与同步接口相同结构
     * @return SseEmitter，Spring MVC 将其作为长连接响应管理，300 秒超时
     */
    public SseEmitter chat(AgentChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 分钟超时，防止僵尸连接
        Integer staffId = securityUtil.getCurrentOperatorId();
        // —— 保存当前认证上下文 ——
        // 下面 new Thread 中的代码不在请求线程内，SecurityContextHolder 默认策略是
        // thread-local，不手动传递则子线程中 getAuthentication() 返回 null
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        AssistantSession session = getOrCreateSession(request, staffId);

        // —— 持久化用户消息 ——
        AssistantMessage userMsg = new AssistantMessage()
                .setSessionId(session.getId()).setRole("USER")
                .setToolMode(session.getMode())
                .setContent(request.getMessage())
                .setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // —— 构建记忆上下文 ——
        String memoryContext = memoryService.buildContext(session);

        String answer;
        try {
            // LLM 调用逻辑与 chatSync 完全一致：system(记忆) + user(消息) + tools
            answer = chatClient.prompt()
                    .system(s -> s.text(memoryContext)) // 注入压缩记忆作为系统上下文
                    .user(request.getMessage())
                    .tools(assistantTools)
                    .call()
                    .content();
        } catch (Exception e) {
            // 捕获所有异常（网络超时、API Key 失效、模型过载等），降级为用户可读消息
            log.error("ChatClient call failed", e);
            answer = "抱歉，AI 服务暂时不可用，请稍后重试。";
        }

        // 防御：即使 LLM 成功返回 content() 也可能为空字符串
        if (answer == null || answer.isBlank()) {
            answer = "抱歉，未能生成回复，请稍后重试。";
        }

        final String finalAnswer = answer; // Lambda 内引用要求 effectively final
        final Long sessionId = session.getId();

        // —— 异步逐字推送 ——
        // 启动新线程而非使用 @Async 注解，因为 SseEmitter 的生命周期必须与请求线程解耦
        new Thread(() -> {
            // 手动恢复认证上下文，确保线程中的 DB 操作（保存消息、更新会话）能通过权限校验
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                // 逐字符发送 token 事件
                for (int i = 0; i < finalAnswer.length(); i++) {
                    String ch = finalAnswer.substring(i, i + 1); // 每次取一个字符
                    emitter.send(SseEmitter.event().name("token").data(ch)); // 事件名 token，前端监听此事件追加字符
                    if (i % 5 == 0) Thread.sleep(5); // 每 5 个字符暂停 5ms，避免推送过快丢失打字节奏
                }
                // —— 全部推送完毕后持久化助手消息 ——
                AssistantMessage assistantMsg = new AssistantMessage()
                        .setSessionId(sessionId).setRole("ASSISTANT")
                        .setToolMode(session.getMode())
                        .setContent(finalAnswer)
                        .setCreateTime(LocalDateTime.now());
                messageMapper.insert(assistantMsg);

                // —— 更新会话统计 ——
                AssistantSession s = sessionMapper.selectById(sessionId); // 重新查询拿最新数据
                if (s != null) {
                    s.setMessageCount(s.getMessageCount() + 2);
                    s.setLastMessageAt(LocalDateTime.now());
                    sessionMapper.updateById(s);
                }
                memoryService.afterMessage(sessionMapper.selectById(sessionId)); // 再次查询确保拿到更新后的 messageCount

                // —— 发送元信息事件 ——
                // event:meta 是自定义事件，标记流结束，携带 conversationId 和 suggestions
                emitter.send(SseEmitter.event().name("meta")
                        .data(Map.of("conversationId", sessionId, "suggestions",
                                List.of("查询我的考勤", "请假流程是什么", "本月薪资明细"))));
                emitter.complete(); // 通知客户端流正常结束
            } catch (Exception e) {
                log.error("SSE send failed", e);
                emitter.completeWithError(e); // 异常结束时客户端 EventSource 触发 onerror
            } finally {
                // 清理线程局部 SecurityContext，防止内存泄漏
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }).start(); // 启动线程后立即返回，不阻塞主线程

        return emitter; // 主线程立即返回 emitter 给 Spring MVC，由其管理异步响应
    }

    /**
     * 会话获取或创建的核心分发逻辑。
     * <p>
     * 三步决策：
     * <ol>
     *   <li>请求带 sessionId 且存在 → 复用已有会话（继续对话）</li>
     *   <li>否则 → 新建会话，同时创建上下文记录</li>
     * </ol>
     * 新建时标题取消息前 50 字符，首个问题通常是"我的考勤情况"这类意图描述，
     * 截取作为标题可读性好于"新会话"。
     * </p>
     */
    private AssistantSession getOrCreateSession(AgentChatRequest request, Integer staffId) {
        // 分支 1：复用已有会话
        if (request.getSessionId() != null) {
            AssistantSession session = sessionMapper.selectById(request.getSessionId()); // 主键查询
            if (session != null) return session; // 找到则直接返回，不校验所有权——后续权限由 staffId 隔离
        }

        // 分支 2：新建会话
        AssistantSession session = new AssistantSession()
                .setStaffId(staffId) // 会话归属于当前员工
                .setTitle(request.getMessage() != null
                        ? request.getMessage().substring(0, Math.min(50, request.getMessage().length())) // 截断防溢出
                        : "新会话")
                .setMode(request.getMode() != null ? request.getMode() : "CHAT") // 默认闲聊模式
                .setStatus("ACTIVE") // 初始状态，后续通过删除接口改为 DELETED
                .setMessageCount(0) // 初始为 0，后续在 chatSync/chat 中 +2
                .setTotalTokens(0L) // 预留字段，当前未实时统计
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session); // MyBatis-Plus 自动回填自增主键到 session.id

        // 同步创建会话上下文——一对一关系，session_id 即 context 的主键
        AssistantSessionContext context = new AssistantSessionContext();
        context.setSessionId(session.getId()); // 与 session 表的主键一致
        context.setContextVersion(0L); // 乐观锁初始版本，AssistantMemoryService 更新时 CAS 比对
        contextMapper.insert(context);

        return session;
    }

    /** 获取当前员工的历史会话列表，按更新时间倒序——最新活跃的会话排最前。 */
    public List<AssistantSession> listSessions() {
        Integer staffId = securityUtil.getCurrentOperatorId(); // 限定当前员工，不暴露他人会话
        return sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AssistantSession>()
                        .eq("staff_id", staffId) // WHERE staff_id = ?
                        .orderByDesc("updated_at")); // ORDER BY updated_at DESC
    }

    /** 按主键获取单个会话元数据。不做所有权校验——调用方应确保只传入当前员工的会话 ID。 */
    public AssistantSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /**
     * 获取会话的消息历史（全部，不做分页）。
     * 按 id 升序排列保证时间顺序——对于自增主键，id 顺序等同于时间顺序。
     */
    public List<AssistantMessage> listMessages(Long sessionId) {
        return messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AssistantMessage>()
                        .eq("session_id", sessionId) // WHERE session_id = ?
                        .orderByAsc("id")); // ORDER BY id ASC —— id 自增保证时间序
    }

    private static final Set<String> VALID_MODES = Set.of("CHAT", "KB_SEARCH"); // 白名单，防止非法模式值写入数据库

    /**
     * 切换会话模式。
     * 仅校验合法性后更新 mode 字段——同一会话内可多次切换，历史消息保留各自的 toolMode 快照。
     */
    public void switchMode(Long sessionId, String mode) {
        if (mode != null && !VALID_MODES.contains(mode)) {
            throw new IllegalArgumentException("不支持的模式: " + mode + "，有效值: CHAT, KB_SEARCH");
        }
        AssistantSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setMode(mode);
            sessionMapper.updateById(session); // 只更新 mode 一个字段
        }
    }

    /**
     * 删除会话——包含级联清理。
     * <p>
     * 删除顺序重要：先删消息（子表），再删上下文（子表），最后删会话（主表）。
     * 依赖数据库外键约束时顺序错误会导致 FK violation；
     * 当前表没有 FK 约束（InnoDB 外键在高并发下有死锁风险），所以由应用层保证顺序。
     * </p>
     */
    @Transactional // 三条删除任一失败则全部回滚，避免出现只删了部分数据的中间状态
    public void deleteSession(Long sessionId) {
        messageMapper.deleteBySessionId(sessionId); // 自定义方法：DELETE FROM assistant_message WHERE session_id = ?
        contextMapper.deleteById(sessionId); // MyBatis-Plus 内置：DELETE FROM assistant_session_context WHERE session_id = ?
        sessionMapper.deleteById(sessionId); // DELETE FROM assistant_session WHERE id = ?
    }
}
