package com.qiujie.knowledge.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 多格式文档解析服务。
 * 支持 PDF / DOCX / MD / TXT，自动编码检测。
 */
@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);

    public String parse(InputStream inputStream, String fileExt) {
        try {
            // 先读入字节数组（支持多次读取）
            byte[] bytes = inputStream.readAllBytes();
            return switch (fileExt.toLowerCase()) {
                case "pdf" -> parsePdf(bytes);
                case "docx" -> parseDocx(bytes);
                case "md", "txt" -> parseText(bytes);
                default -> throw new IllegalArgumentException("不支持的文件类型: " + fileExt);
            };
        } catch (IOException e) {
            throw new RuntimeException("文档解析失败: " + fileExt, e);
        }
    }

    private String parsePdf(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String parseDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String parseText(byte[] bytes) {
        Charset charset = detectCharset(bytes);
        return new String(bytes, charset);
    }

    /**
     * 自动编码检测：尝试 UTF-8 → GBK → UTF-16
     */
    private Charset detectCharset(byte[] bytes) {
        // BOM 检测
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        // 尝试 UTF-8 解码
        try {
            String test = new String(bytes, StandardCharsets.UTF_8);
            if (test.equals(new String(test.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))) {
                return StandardCharsets.UTF_8;
            }
        } catch (Exception ignored) {}
        // 降级到 GBK
        return Charset.forName("GBK");
    }
}
