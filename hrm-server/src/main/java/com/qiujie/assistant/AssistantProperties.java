package com.qiujie.assistant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {

    private boolean enabled = true;

    private int timeoutSeconds = 15;

    private int retentionDays = 30;

    private Provider provider = new Provider();

    @Data
    public static class Provider {
        private String baseUrl = "";
        private String apiKey = "";
        private String model = "";
    }
}
