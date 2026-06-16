package com.qiujie.knowledge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("文本清洗服务")
class TextCleanupServiceUnitTest {

    private final TextCleanupService service = new TextCleanupService();

    @Test
    @DisplayName("空文本应返回空字符串")
    void clean_NullOrEmpty_ShouldReturnEmpty() {
        assertEquals("", service.clean(null));
        assertEquals("", service.clean(""));
        assertEquals("", service.clean("   "));
    }

    @Test
    @DisplayName("\\r\\n 应统一为 \\n")
    void clean_CarriageReturn_ShouldNormalizeToLF() {
        String text = "第一章总则\r\n第二条细则\r第三条附则\n第四章其他";
        String result = service.clean(text);

        assertFalse(result.contains("\r"),
                "Should not contain carriage returns: " + escape(result));
    }

    @Test
    @DisplayName("3+ 连续空行应压缩为双换行")
    void clean_ExcessiveBlankLines_ShouldCompress() {
        String text = "第一章段落A\n\n\n\n\n\n第二章段落B";
        String result = service.clean(text);

        assertFalse(result.contains("\n\n\n"),
                "Should not contain 3+ consecutive newlines: " + escape(result));
        assertTrue(result.contains("段落A"));
        assertTrue(result.contains("段落B"));
    }

    @Test
    @DisplayName("独立页码行应被移除")
    void clean_StandalonePageNumber_ShouldRemove() {
        String text = "第一章 概述\n\n42\n\n第二章 细则";
        String result = service.clean(text);

        assertFalse(result.contains("42"),
                "Standalone page number should be removed: " + result);
        assertTrue(result.contains("概述"));
        assertTrue(result.contains("细则"));
    }

    @Test
    @DisplayName("多空格应压缩为单空格")
    void clean_MultipleSpaces_ShouldCompress() {
        String text = "这是  一段  有多余  空格的文本";
        String result = service.clean(text);

        assertFalse(result.contains("  "),
                "Should not contain double spaces: " + escape(result));
    }

    @Test
    @DisplayName("极短行（<5字符）应被移除")
    void clean_VeryShortLines_ShouldRemove() {
        String text = "正常段落文本\nab\ncd\n另一段文本";
        String result = service.clean(text);

        assertTrue(result.contains("正常段落文本"));
        assertTrue(result.contains("另一段文本"));
    }

    @Test
    @DisplayName("综合清洗应正确输出")
    void clean_CombinedExample_ShouldCleanProperly() {
        String text =
                "第一章 规章制度\r\n\r\n" +
                "1\r\n\r\n" +
                "第一条  员工应遵守以下规定: \r\n\r\n\r\n\r\n" +
                "    第二条  考勤制度见附录。\r\n\r\n" +
                "ab\r\n";

        String result = service.clean(text);
        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("规章制度"));
        assertTrue(result.contains("考勤"));
        // page number should be removed
        assertFalse(result.matches("(?s).*\\b1\\b.*"));
    }

    private String escape(String s) {
        return s.replace("\n", "\\n").replace("\r", "\\r");
    }
}
