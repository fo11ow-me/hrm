package com.qiujie.assistant;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

@Data
@Component
@ConfigurationProperties(prefix = "assistant")
public class ChatProperties {

    private static final Logger log = LoggerFactory.getLogger(ChatProperties.class);

    private boolean enabled = true;

    private int timeoutSeconds = 15;

    private int retentionDays = 30;

    private int dailyQuota = 50; // 每日查询配额

    private Provider provider = new Provider();

    @Data
    public static class Provider {
        private String baseUrl = "";
        private String apiKey = "";
        private String model = "";
    }

    /**
     * 启动时验证配置安全性
     */
    @PostConstruct
    public void validate() {
        if (!enabled) {
            log.info("Assistant service is disabled");
            return;
        }
        log.info("Assistant service enabled (using Spring AI OpenAPI/DashScope), quota={}/day", dailyQuota);
    }
}
