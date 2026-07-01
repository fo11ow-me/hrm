package com.qiujie.assistant.service;

import com.qiujie.assistant.entity.ChatLlmUsage;
import com.qiujie.assistant.mapper.ChatLlmUsageMapper;
import com.qiujie.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * LLM 调用统计服务，记录每次 LLM 调用的用量和性能指标。
 *
 * @author quuj
 */
@Service
public class ChatLlmUsageService {

    private static final Logger log = LoggerFactory.getLogger(ChatLlmUsageService.class);

    @Autowired
    private ChatLlmUsageMapper usageMapper;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 记录一次 LLM 调用。
     */
    public void record(String module, String endpoint, Long sessionId,
                        int promptTokens, int completionTokens, boolean isEstimated,
                        long latencyMs, boolean success, String errorMessage, String modelName) {
        try {
            ChatLlmUsage usage = new ChatLlmUsage();
            usage.setStaffId(securityUtil.getCurrentOperatorId());
            usage.setModule(module);
            usage.setEndpoint(endpoint);
            usage.setSessionId(sessionId);
            usage.setPromptTokens(promptTokens);
            usage.setCompletionTokens(completionTokens);
            usage.setTotalTokens(promptTokens + completionTokens);
            usage.setIsEstimated(isEstimated);
            usage.setCostAmount(estimateCost(promptTokens, completionTokens, modelName));
            usage.setCostCurrency("CNY");
            usage.setLatencyMs(latencyMs);
            usage.setSuccess(success);
            usage.setErrorMessage(errorMessage);
            usage.setModelName(modelName);
            usage.setCreateTime(LocalDateTime.now());
            usageMapper.insert(usage);
        } catch (Exception e) {
            log.warn("Failed to save LLM usage record", e);
        }
    }

    /**
     * 估算单次调用费用（参考阿里云百炼 qwen-plus 价格）。
     */
    private BigDecimal estimateCost(int promptTokens, int completionTokens, String modelName) {
        // qwen-plus: 输入 0.0008 元/千token, 输出 0.002 元/千token
        double promptCost = promptTokens / 1000.0 * 0.0008;
        double completionCost = completionTokens / 1000.0 * 0.002;
        return BigDecimal.valueOf(promptCost + completionCost).setScale(6, java.math.RoundingMode.HALF_UP);
    }
}
