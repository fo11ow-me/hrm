package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.entity.AssistantMessage;
import com.qiujie.mapper.AssistantMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Token 使用量管理服务
 *
 * @author qiujie
 * @date 2026-06-09
 */
@Service
public class TokenUsageService {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageService.class);

    private static final String DAILY_USAGE_KEY_PREFIX = "assistant:usage:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AssistantMessageMapper messageMapper;

    /**
     * 获取用户今日使用量
     */
    public int getDailyUsage(Integer staffId) {
        String key = buildDailyKey(staffId);
        String count = redisTemplate.opsForValue().get(key);
        return count == null ? 0 : Integer.parseInt(count);
    }

    /**
     * 记录使用量
     */
    public void recordUsage(Integer staffId, int questionLength, int answerLength) {
        String key = buildDailyKey(staffId);

        // 增加计数
        redisTemplate.opsForValue().increment(key);

        // 设置过期时间(保留到当天结束)
        long secondsUntilMidnight = getSecondsUntilMidnight();
        redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);

        // 记录详细日志
        log.info("Assistant usage: staffId={}, questionLength={}, answerLength={}",
                 staffId, questionLength, answerLength);
    }

    /**
     * 检查是否超过配额
     */
    public boolean isQuotaExceeded(Integer staffId, int maxQuota) {
        int usage = getDailyUsage(staffId);
        if (usage >= maxQuota) {
            log.warn("Quota exceeded: staffId={}, usage={}, maxQuota={}",
                     staffId, usage, maxQuota);
            return true;
        }
        return false;
    }

    /**
     * 从数据库获取历史使用统计(用于备份)
     */
    public int getHistoricalUsage(Integer staffId, LocalDate date) {
        Timestamp startTime = Timestamp.valueOf(date.atStartOfDay());
        Timestamp endTime = Timestamp.valueOf(date.plusDays(1).atStartOfDay());

        Long count = messageMapper.selectCount(new QueryWrapper<AssistantMessage>()
            .eq("staff_id", staffId)
            .ge("create_time", startTime)
            .lt("create_time", endTime)
            .eq("role", "USER"));

        return count == null ? 0 : count.intValue();
    }

    private String buildDailyKey(Integer staffId) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return DAILY_USAGE_KEY_PREFIX + staffId + ":" + today;
    }

    private long getSecondsUntilMidnight() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        long secondsUntilMidnight = tomorrow.atStartOfDay()
            .toEpochSecond(java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now()));
        long currentSeconds = System.currentTimeMillis() / 1000;
        return secondsUntilMidnight - currentSeconds;
    }
}