package com.qiujie.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.qiujie.enums.BaseEnum;
import org.apache.poi.ss.formula.functions.T;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EasyExcel 同步读写工具类，替代 HutoolExcelUtil。
 * 内部使用 EasyExcel 的流式 SAX 解析，内存占用远低于 Hutool 的 DOM 模式。
 *
 * @author qiujie
 */
public class EasyExcelUtil {

    /**
     * 同步导出——将数据列表直接写入 HTTP 响应流，浏览器触发下载。
     * 适用于小数据量场景，大数据量请使用 FileTaskEngine 异步导出。
     *
     * @param response HTTP 响应
     * @param data     导出数据列表
     * @param filename 文件名（不含扩展名）
     * @param clazz    Excel 行对应的 DTO/VO 类型
     */
    public static <T> void write(HttpServletResponse response, List<T> data, String filename, Class<T> clazz) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + ".xlsx");
        List<Field> fields = resolveExcelFields(clazz);
        List<List<String>> head = fields.stream()
                .map(field -> Collections.singletonList(resolveHeadName(field)))
                .collect(Collectors.toList());
        List<List<Object>> rows = new ArrayList<>();
        for (T item : data) {
            List<Object> row = new ArrayList<>();
            for (Field field : fields) {
                row.add(readCellValue(item, field));
            }
            rows.add(row);
        }
        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("data")
                .doWrite(rows);
    }

    /**
     * 同步读取 Excel 文件，全部行加载到内存。
     * 适用于小文件（< 1000 行），大文件请使用 FileTaskEngine 异步导入。
     *
     * @param inputStream   Excel 文件输入流
     * @param headRowNumber 表头行号（从 1 开始）
     * @param clazz         Excel 行对应的 DTO 类型
     * @return 解析后的数据列表
     */
    public static <T> List<T> read(InputStream inputStream, int headRowNumber, Class<T> clazz) {
        BufferedInputStream bufferedInputStream = ensureSupportedExcelStream(inputStream);
        List<T> rows = EasyExcel.read(bufferedInputStream)
                .head(clazz)
                .headRowNumber(headRowNumber)
                .sheet()
                .doReadSync();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Excel file has no data rows");
        }
        return rows;
    }

    private static BufferedInputStream ensureSupportedExcelStream(InputStream inputStream) {
        BufferedInputStream bufferedInputStream = inputStream instanceof BufferedInputStream
                ? (BufferedInputStream) inputStream
                : new BufferedInputStream(inputStream);
        try {
            bufferedInputStream.mark(4);
            byte[] header = new byte[4];
            int len = bufferedInputStream.read(header);
            bufferedInputStream.reset();
            if (len < 4 || header[0] != 0x50 || header[1] != 0x4B) {
                throw new IllegalArgumentException("Only valid xlsx files are supported");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read Excel file", e);
        }
        return bufferedInputStream;
    }

    private static List<Field> resolveExcelFields(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        boolean hasExcelProperty = false;
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(ExcelProperty.class)) {
                hasExcelProperty = true;
                break;
            }
        }
        List<Field> fields = new ArrayList<>();
        for (Field field : declaredFields) {
            if (shouldSkipField(field, hasExcelProperty)) {
                continue;
            }
            field.setAccessible(true);
            fields.add(field);
        }
        return fields;
    }

    private static boolean shouldSkipField(Field field, boolean onlyAnnotated) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isAnnotationPresent(ExcelIgnore.class)) {
            return true;
        }
        if (onlyAnnotated && !field.isAnnotationPresent(ExcelProperty.class)) {
            return true;
        }
        TableField tableField = field.getAnnotation(TableField.class);
        return tableField != null && !tableField.exist();
    }

    private static String resolveHeadName(Field field) {
        ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
        if (excelProperty != null && excelProperty.value().length > 0 && !"".equals(excelProperty.value()[0])) {
            return excelProperty.value()[0];
        }
        return field.getName();
    }

    private static Object readCellValue(Object item, Field field) {
        try {
            Object value = field.get(item);
            if (value instanceof BaseEnum) {
                return ((BaseEnum<T>) value).getMessage();
            }
            if (value instanceof Enum) {
                return ((Enum<?>) value).name();
            }
            if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
                return value.toString();
            }
            return value;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to read Excel field: " + field.getName(), e);
        }
    }
}
