package com.qiujie.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.assistant.entity.AgentMessage;
import com.qiujie.assistant.entity.AgentSession;
import com.qiujie.assistant.mapper.AgentMessageMapper;
import com.qiujie.assistant.mapper.AgentSessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Agent 三级记忆服务")
class AgentMemoryServiceUnitTest {

    @Mock
    private AgentSessionMapper sessionMapper;

    @Mock
    private AgentMessageMapper messageMapper;

    @InjectMocks
    private AgentMemoryService memoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(memoryService, "l1UpdateInterval", 5);
        ReflectionTestUtils.setField(memoryService, "l2UpdateInterval", 15);
        ReflectionTestUtils.setField(memoryService, "maxTokens", 1000);
        ReflectionTestUtils.setField(memoryService, "keepRecent", 5);
    }

    @Test
    @DisplayName("空会话应返回空上下文")
    void buildContext_EmptySession_ShouldReturnEmpty() {
        AgentSession session = new AgentSession();

        String context = memoryService.buildContext(session);

        assertTrue(context.isBlank());
    }

    @Test
    @DisplayName("有 L1 记忆时应包含会话记忆")
    void buildContext_WithL1Memory_ShouldIncludeSessionMemory() {
        AgentSession session = new AgentSession()
                .setSessionMemory("用户询问了年假政策，确认每年5天");

        String context = memoryService.buildContext(session);

        assertTrue(context.contains("年假政策"));
        assertTrue(context.contains("当前会话关键信息"));
    }

    @Test
    @DisplayName("有 L2 紧凑摘要时应排在前面")
    void buildContext_WithL2Summary_ShouldAppearFirst() {
        AgentSession session = new AgentSession()
                .setCompactSummary("历史对话涉及: 考勤、薪资、请假")
                .setSessionMemory("本轮: 询问年假");

        String context = memoryService.buildContext(session);

        int l2Index = context.indexOf("历史对话精要");
        int l1Index = context.indexOf("当前会话关键信息");
        assertTrue(l2Index < l1Index,
                "L2 compact summary should appear before L1 session memory");
    }

    @Test
    @DisplayName("消息超过 token 限制应截断")
    void getRecentMessages_TokenOverflow_ShouldTruncate() {
        ReflectionTestUtils.setField(memoryService, "maxTokens", 50); // low threshold
        List<AgentMessage> messages = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            messages.add(new AgentMessage()
                    .setId((long) i)
                    .setRole(i % 2 == 0 ? "user" : "assistant")
                    .setContent("这是用于测试token计数功能的长消息内容")
                    .setCreatedAt(LocalDateTime.now()));
        }

        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(messages);

        List<AgentMessage> result = memoryService.getRecentMessages(1L);

        assertFalse(result.isEmpty());
        assertTrue(result.size() <= 5 + 5, // keepRecent or less
                "Truncated size should be limited, got " + result.size());
    }

    @Test
    @DisplayName("消息未超 token 限制应全部返回")
    void getRecentMessages_UnderTokenLimit_ShouldReturnAll() {
        List<AgentMessage> messages = List.of(
                new AgentMessage().setId(1L).setRole("user").setContent("简短问题").setCreatedAt(LocalDateTime.now()),
                new AgentMessage().setId(2L).setRole("assistant").setContent("简短回答").setCreatedAt(LocalDateTime.now())
        );

        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(messages);

        List<AgentMessage> result = memoryService.getRecentMessages(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("消息达到 L1 更新间隔应更新会话")
    void afterMessage_AtL1Interval_ShouldUpdateSession() {
        AgentSession session = new AgentSession()
                .setId(1L)
                .setMessageCount(4) // one before L1 interval (5)
                .setUpdatedAt(LocalDateTime.now());

        when(sessionMapper.updateById(any(AgentSession.class))).thenReturn(1);

        memoryService.afterMessage(session);

        assertEquals(5, session.getMessageCount());
        assertNotNull(session.getUpdatedAt());
    }

    @Test
    @DisplayName("buildContext 不存在 L1/L2 时应返回空")
    void buildContext_NoMemory_ShouldReturnEmpty() {
        AgentSession session = new AgentSession();

        String ctx = memoryService.buildContext(session);

        assertEquals("", ctx.trim());
    }
}
