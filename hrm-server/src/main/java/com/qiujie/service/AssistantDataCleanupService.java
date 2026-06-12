package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.entity.AssistantConversation;
import com.qiujie.entity.AssistantMessage;
import com.qiujie.entity.AssistantToolCall;
import com.qiujie.mapper.AssistantConversationMapper;
import com.qiujie.mapper.AssistantMessageMapper;
import com.qiujie.mapper.AssistantToolCallMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 助手历史数据定时清理。
 *
 * @author qiujie
 */
@Service
public class AssistantDataCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AssistantDataCleanupService.class);
    private static final int BATCH_SIZE = 100;

    @Value("${assistant.retention-days:30}")
    private int retentionDays;

    @Autowired
    private AssistantConversationMapper conversationMapper;

    @Autowired
    private AssistantMessageMapper messageMapper;

    @Autowired
    private AssistantToolCallMapper toolCallMapper;

    @Scheduled(cron = "0 0 5 * * ?")
    public void cleanExpiredConversations() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
        QueryWrapper<AssistantConversation> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("update_time", Timestamp.valueOf(expireTime));
        List<AssistantConversation> expired = conversationMapper.selectList(queryWrapper);

        if (expired.isEmpty()) {
            log.debug("无过期会话需要清理");
            return;
        }

        List<Long> ids = expired.stream().map(AssistantConversation::getId).collect(Collectors.toList());
        int deletedMessages = 0;
        int deletedToolCalls = 0;

        for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, ids.size());
            List<Long> batch = ids.subList(i, end);

            toolCallMapper.delete(new QueryWrapper<AssistantToolCall>().in("conversation_id", batch));
            deletedToolCalls += batch.size();

            deletedMessages += messageMapper.delete(new QueryWrapper<AssistantMessage>().in("conversation_id", batch));
        }

        int deletedConversations = conversationMapper.deleteBatchIds(ids);

        log.info("清理助手历史数据: 会话 {} 个, 消息约 {} 条, 工具调用约 {} 批",
                deletedConversations, deletedMessages, deletedToolCalls);
    }
}
