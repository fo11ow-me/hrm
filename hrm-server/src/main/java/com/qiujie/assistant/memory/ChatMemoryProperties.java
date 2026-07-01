package com.qiujie.assistant.memory;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryProperties {

    private int l1MessageTrigger = 4;
    private int l1TokenTrigger = 1200;
    private int l2MessageTrigger = 6;
    private int l2TokenTrigger = 1800;
    /** L2 紧凑摘要最大 token 数（FIFO 环形） */
    private int compactSummaryMaxTokens = 2400;
    private int maxTokens = 50000;
    private int keepRecent = 3;
}
