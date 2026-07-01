package com.qiujie.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatProperties 安全配置测试
 *
 * @author qiujie
 * @date 2026-06-09
 */
@DisplayName("助手服务安全配置测试")
class ChatPropertiesTest {

    private ChatProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ChatProperties();
    }

    // ==================== API Key 验证测试 ====================

    @Test
    @DisplayName("启用助手但未配置 API Key 应抛出异常")
    void testValidate_EnabledButNoApiKey() {
        properties.setEnabled(true);
        properties.getProvider().setBaseUrl("https://api.openai.com");
        properties.getProvider().setModel("gpt-3.5-turbo");
        // API Key 为空

        assertThrows(IllegalStateException.class, () -> properties.validate());
    }

    @Test
    @DisplayName("启用助手但未配置 BaseUrl 应抛出异常")
    void testValidate_EnabledButNoBaseUrl() {
        properties.setEnabled(true);
        properties.getProvider().setApiKey("sk-test-key");
        properties.getProvider().setModel("gpt-3.5-turbo");
        // BaseUrl 为空

        assertThrows(IllegalStateException.class, () -> properties.validate());
    }

    @Test
    @DisplayName("启用助手但未配置 Model 应抛出异常")
    void testValidate_EnabledButNoModel() {
        properties.setEnabled(true);
        properties.getProvider().setApiKey("sk-test-key");
        properties.getProvider().setBaseUrl("https://api.openai.com");
        // Model 为空

        assertThrows(IllegalStateException.class, () -> properties.validate());
    }

    @Test
    @DisplayName("禁用助手时无需验证配置")
    void testValidate_Disabled() {
        properties.setEnabled(false);
        properties.getProvider().setApiKey(""); // 即使为空

        // 不应抛出异常
        assertDoesNotThrow(() -> properties.validate());
    }

    @Test
    @DisplayName("完整配置应验证成功")
    void testValidate_CompleteConfig() {
        properties.setEnabled(true);
        properties.getProvider().setApiKey("sk-test-key");
        properties.getProvider().setBaseUrl("https://api.openai.com");
        properties.getProvider().setModel("gpt-3.5-turbo");

        // 应验证成功
        assertDoesNotThrow(() -> properties.validate());
    }

    @Test
    @DisplayName("使用环境变量占位符不应触发警告")
    void testValidate_EnvironmentVariable() {
        properties.setEnabled(true);
        properties.getProvider().setApiKey("${ASSISTANT_API_KEY}");
        properties.getProvider().setBaseUrl("https://api.openai.com");
        properties.getProvider().setModel("gpt-3.5-turbo");

        // 应验证成功,且不触发硬编码警告
        assertDoesNotThrow(() -> properties.validate());
    }

    @Test
    @DisplayName("每日配额应配置合理值")
    void testDailyQuota_Default() {
        assertEquals(50, properties.getDailyQuota(), "默认每日配额应为 50");
    }

    @Test
    @DisplayName("超时时间应配置合理值")
    void testTimeout_Default() {
        assertEquals(15, properties.getTimeoutSeconds(), "默认超时时间应为 15 秒");
    }
}
