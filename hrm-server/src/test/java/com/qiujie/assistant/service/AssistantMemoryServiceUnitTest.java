package com.qiujie.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.assistant.entity.AssistantMessage;
import com.qiujie.assistant.entity.AssistantSession;
import com.qiujie.assistant.entity.AssistantSessionContext;
import com.qiujie.assistant.mapper.AssistantMessageMapper;
import com.qiujie.assistant.mapper.AssistantSessionContextMapper;
import com.qiujie.assistant.mapper.AssistantSessionMapper;
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
class AssistantMemoryServiceUnitTest {

    @Mock
    private AssistantSessionMapper sessionMapper;

    @Mock
    private AssistantMessageMapper messageMapper;

    @Mock
    private AssistantSessionContextMapper contextMapper;

    @InjectMocks
    private AssistantMemoryService memoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(memoryService, "l1UpdateInterval", 5);
        ReflectionTestUtils.setField(memoryService, "l2UpdateInterval", 15);
        ReflectionTestUtils.setField(memoryService, "maxTokens", 1000);
        ReflectionTestUtils.setField(memoryService, "keepRecent", 5);
    }

    @Test
    @DisplayName("空上下文应返回空")
    void buildContext_EmptyContext_ShouldReturnEmpty() {
        AssistantSession session = new AssistantSession().setId(1L);

        when(contextMapper.selectById(1L)).thenReturn(null);

        String context = memoryService.buildContext(session);

        assertEquals("", context.trim());
    }

    @Test
    @DisplayName("有 L1 记忆时应包含会话记忆")
    void buildContext_WithL1Memory_ShouldIncludeSessionMemory() {
        AssistantSession session = new AssistantSession().setId(1L);
        AssistantSessionContext ctx = new AssistantSessionContext();
        ctx.setSessionMemory("用户询问了年假政策，确认每年5天");

        when(contextMapper.selectById(1L)).thenReturn(ctx);

        String context = memoryService.buildContext(session);

        assertTrue(context.contains("年假政策"));
        assertTrue(context.contains("当前会话关键信息"));
    }

    @Test
    @DisplayName("有 L2 紧凑摘要时应排在前面")
    void buildContext_WithL2Summary_ShouldAppearFirst() {
        AssistantSession session = new AssistantSession().setId(1L);
        AssistantSessionContext ctx = new AssistantSessionContext();
        ctx.setCompactSummary("历史对话涉及: 考勤、薪资、请假");
        ctx.setSessionMemory("本轮: 询问年假");

        when(contextMapper.selectById(1L)).thenReturn(ctx);

        String context = memoryService.buildContext(session);

        int l2Index = context.indexOf("历史对话精要");
        int l1Index = context.indexOf("当前会话关键信息");
        assertTrue(l2Index < l1Index,
                "L2 compact summary should appear before L1 session memory");
    }

    @Test
    @DisplayName("消息超过 token 限制应截断")
    void getRecentMessages_TokenOverflow_ShouldTruncate() {
        ReflectionTestUtils.setField(memoryService, "maxTokens", 50);
        List<AssistantMessage> messages = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            messages.add(new AssistantMessage()
                    .setId((long) i)
                    .setRole(i % 2 == 0 ? "USER" : "ASSISTANT")
                    .setContent("这是用于测试token计数功能的长消息内容")
                    .setCreateTime(LocalDateTime.now()));
        }

        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(messages);

        List<AssistantMessage> result = memoryService.getRecentMessages(1L);

        assertFalse(result.isEmpty());
        assertTrue(result.size() < 20,
                "Truncated size should be limited, got " + result.size());
    }

    @Test
    @DisplayName("消息未超 token 限制应全部返回")
    void getRecentMessages_UnderTokenLimit_ShouldReturnAll() {
        List<AssistantMessage> messages = List.of(
                new AssistantMessage().setId(1L).setRole("USER").setContent("简短问题").setCreateTime(LocalDateTime.now()),
                new AssistantMessage().setId(2L).setRole("ASSISTANT").setContent("简短回答").setCreateTime(LocalDateTime.now())
        );

        when(messageMapper.selectList(any(QueryWrapper.class))).thenReturn(messages);

        List<AssistantMessage> result = memoryService.getRecentMessages(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("消息达到 L1 更新间隔应更新会话")
    void afterMessage_AtL1Interval_ShouldUpdateSession() {
        AssistantSession session = new AssistantSession()
                .setId(1L)
                .setMessageCount(4)
                .setUpdateTime(LocalDateTime.now());

        when(sessionMapper.updateById(any(AssistantSession.class))).thenReturn(1);

        memoryService.afterMessage(session);

        assertEquals(5, session.getMessageCount());
        assertNotNull(session.getUpdateTime());
    }
}
