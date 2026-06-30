package com.qiujie.assistant.memory;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 助理记忆结构化配置
 *
 * @author qiujie
 * @since 2026-06-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.memory")
public class AssistantMemoryProperties {

    private int l1MessageTrigger = 4;
    private int l1TokenTrigger = 1200;
    private int l2MessageTrigger = 6;
    private int l2TokenTrigger = 1800;
    private int sessionTokenThreshold = 6500;
    private int maxTokens = 50000;
    private int keepRecent = 3;
}
