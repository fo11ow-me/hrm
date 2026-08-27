package com.qiujie.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.qiujie.enums.TaskModuleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 通用 Excel 导入引擎：表头自动匹配 → 反射设值。
 * <p>
 * 不依赖 {@code @ExcelProperty} 注解，列序无关，多余列自动跳过。
 * 映射来源：ColumnMappingRegistry（精确）→ AI 兜底（模糊）。
 *
 * @author qiujie
 */
public final class FlexibleExcelImporter {

    private static final Logger log = LoggerFactory.getLogger(FlexibleExcelImporter.class);
    private static final int DEFAULT_HEAD_ROW = 1;

    private FlexibleExcelImporter() {}

    /**
     * 解析 Excel 为实体列表。
     *
     * @param inputStream  Excel 文件流
     * @param module       导入模块（用于查映射表）
     * @param entityClass  目标实体类
     * @param <T>          实体类型
     * @return 实体列表（解析失败的会包含在错误信息中）
     */
    public static <T> List<ImportResult<T>> parse(InputStream inputStream, TaskModuleEnum module,
                                                    Class<T> entityClass) {
        return parse(inputStream, DEFAULT_HEAD_ROW, module, entityClass, null);
    }

    /**
     * 解析 Excel。
     *
     * @param inputStream  Excel 文件流
     * @param headRow      表头行号（从 1 开始）
     * @param module       导入模块
     * @param entityClass  目标实体类
     * @param fallback     AI 兜底匹配器，可为 null（跳过来知列）
     * @param <T>          实体类型
     */
    public static <T> List<ImportResult<T>> parse(InputStream inputStream, int headRow,
                                                    TaskModuleEnum module, Class<T> entityClass,
                                                    AiHeaderMatcher fallback) {
        List<ImportResult<T>> results = new ArrayList<>();
        read(inputStream, headRow, module, entityClass, fallback, results::add);
        return results;
    }

    /**
     * 流式解析 Excel，每解析一行立即回调，不保留完整结果列表。
     *
     * @param inputStream Excel 文件流
     * @param headRow 表头行号（从 1 开始）
     * @param module 导入模块
     * @param entityClass 目标实体类
     * @param fallback AI 兜底匹配器，可为 null
     * @param resultConsumer 单行解析结果回调
     */
    public static <T> void read(InputStream inputStream, int headRow,
                                TaskModuleEnum module, Class<T> entityClass,
                                AiHeaderMatcher fallback,
                                java.util.function.Consumer<ImportResult<T>> resultConsumer) {
        FlexibleListener<T> listener = new FlexibleListener<>(
                headRow, module, entityClass, fallback, resultConsumer);
        EasyExcel.read(inputStream, listener).sheet().headRowNumber(headRow).doRead();
    }

    /**
     * 导入结果：成功时 entity != null；失败时 entity == null，error 有值。
     */
    public static class ImportResult<T> {
        private final int rowNum;
        private final T entity;
        private final String error;

        ImportResult(int rowNum, T entity, String error) {
            this.rowNum = rowNum;
            this.entity = entity;
            this.error = error;
        }

        public int getRowNum() { return rowNum; }
        public T getEntity() { return entity; }
        public String getError() { return error; }
        public boolean isSuccess() { return entity != null; }
    }

    /**
     * AI 兜底匹配接口：传入未知列名 + 可用字段列表，返回匹配的字段名或 null。
     */
    @FunctionalInterface
    public interface AiHeaderMatcher {
        String match(String headerText, List<String> availableFields);
    }

    // ==================== internal ====================

    private static class FlexibleListener<T> extends AnalysisEventListener<Map<Integer, String>> {
        private final int headRow;
        private final TaskModuleEnum module;
        private final Class<T> entityClass;
        private final AiHeaderMatcher fallback;
        private final java.util.function.Consumer<ImportResult<T>> resultConsumer;
        private final Map<Integer, String> columnIndexToField = new LinkedHashMap<>();
        private int rowCount;

        FlexibleListener(int headRow, TaskModuleEnum module, Class<T> entityClass,
                         AiHeaderMatcher fallback,
                         java.util.function.Consumer<ImportResult<T>> resultConsumer) {
            this.headRow = headRow;
            this.module = module;
            this.entityClass = entityClass;
            this.fallback = fallback;
            this.resultConsumer = resultConsumer;
        }

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext ctx) {
            Map<String, String> mapping = ColumnMappingRegistry.get(module);
            List<String> availableFields = getSettableFields(entityClass);

            for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
                String header = entry.getValue() != null ? entry.getValue().trim() : "";
                String fieldName = mapping.get(header);

                // 映射表未命中 → 尝试模糊匹配（去空格、统一标点）
                if (fieldName == null) {
                    String normalized = normalizeHeader(header);
                    fieldName = mapping.entrySet().stream()
                            .filter(e -> normalizeHeader(e.getKey()).equals(normalized))
                            .map(Map.Entry::getValue).findFirst().orElse(null);
                }

                // 仍未命中 → AI 兜底
                if (fieldName == null && fallback != null) {
                    String aiMatch = fallback.match(header, availableFields);
                    if (aiMatch != null && availableFields.contains(aiMatch)) {
                        fieldName = aiMatch;
                        log.info("AI兜底匹配: {} → {}", header, fieldName);
                    }
                }

                if (fieldName != null) {
                    columnIndexToField.put(entry.getKey(), fieldName);
                } else {
                    log.debug("未映射的列: {}", header);
                }
            }
            log.info("表头映射完成: {} 列中 {} 列已匹配", headMap.size(), columnIndexToField.size());
        }

        @Override
        public void invoke(Map<Integer, String> row, AnalysisContext ctx) {
            rowCount++;
            try {
                T entity = entityClass.getDeclaredConstructor().newInstance();
                for (Map.Entry<Integer, String> col : columnIndexToField.entrySet()) {
                    String rawValue = row.get(col.getKey());
                    if (rawValue == null || rawValue.trim().isEmpty()) continue;
                    setFieldValue(entity, col.getValue(), rawValue.trim());
                }
                resultConsumer.accept(new ImportResult<>(rowCount, entity, null));
            } catch (Exception e) {
                resultConsumer.accept(new ImportResult<>(rowCount, null, e.getMessage()));
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext ctx) {
            log.info("导入完成: {} 行", rowCount);
        }

        public List<ImportResult<T>> getResults() {
            throw new UnsupportedOperationException("Streaming listener does not retain results");
        }
    }

    /** 获取实体类所有有 setter 的字段名 */
    private static <T> List<String> getSettableFields(Class<T> clazz) {
        List<String> fields = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())
                    && !"serialVersionUID".equals(f.getName())) {
                fields.add(f.getName());
            }
        }
        return fields;
    }

    /** 表头归一化：去空格、全角转半角、去多余符号 */
    private static String normalizeHeader(String header) {
        if (header == null) return "";
        return header.trim()
                .replace("（", "(").replace("）", ")")
                .replace("　", " ")
                .replaceAll("[ 　]+", "");
    }

    /** 反射设值：根据字段名推断类型并转换 */
    private static <T> void setFieldValue(T entity, String fieldName, String rawValue) throws Exception {
        Field field = findField(entity.getClass(), fieldName);
        if (field == null) return;
        field.setAccessible(true);
        Class<?> type = field.getType();
        field.set(entity, convert(rawValue, type, field));
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(String value, Class<?> type, Field field) {
        if (value == null || value.isEmpty()) return null;
        try {
            if (type == String.class) return value;
            if (type == Integer.class || type == int.class) return Integer.valueOf(cleanNumber(value));
            if (type == Long.class || type == long.class) return Long.valueOf(cleanNumber(value));
            if (type == BigDecimal.class) return new BigDecimal(cleanNumber(value));
            // 日期时间
            if (type == Date.class || type == java.util.Date.class || type == Timestamp.class) {
                return parseDateOrTime(value, type);
            }
            // 如果是枚举，尝试匹配
            if (type.isEnum()) {
                return parseEnum(type, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("无法将 '" + value + "' 转为 " + type.getSimpleName(), e);
        }
        return value;
    }

    private static String cleanNumber(String value) {
        return value.replace(",", "").replace("，", "").trim();
    }

    private static Object parseDateOrTime(String value, Class<?> type) {
        try {
            // Excel 日期序列号 (如 46225)
            if (value.matches("\\d{4,6}(\\.\\d+)?")) {
                double d = Double.parseDouble(value);
                long days = (long) d;
                // Excel 日期起始 1899-12-30
                java.util.Date base = new SimpleDateFormat("yyyy-MM-dd").parse("1899-12-30");
                long ms = base.getTime() + days * 86400000L;
                if (d != days) {
                    ms += (long) ((d - days) * 86400000);
                }
                if (type == Date.class) return new Date(ms);
                if (type == Timestamp.class) return new Timestamp(ms);
                return new java.util.Date(ms);
            }
            // 常见日期格式
            String[][] patterns = {
                    {"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "M/d/yy HH:mm"},
                    {"yyyy-MM-dd", "yyyy/MM/dd", "M/d/yy", "yyyy-M-d"}
            };
            int patternGroup = value.contains(":") ? 0 : 1;
            for (String pat : patterns[patternGroup]) {
                try {
                    java.util.Date d = new SimpleDateFormat(pat).parse(value);
                    if (type == Date.class) return new Date(d.getTime());
                    if (type == Timestamp.class) return new Timestamp(d.getTime());
                    return d;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("日期格式无法识别: " + value, e);
        }
        throw new RuntimeException("日期格式无法识别: " + value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(Class type, String value) {
        for (Object constant : type.getEnumConstants()) {
            if (constant.toString().equals(value)) return constant;
            try {
                Field codeField = type.getDeclaredField("code");
                codeField.setAccessible(true);
                Object code = codeField.get(constant);
                if (code != null && code.toString().equals(value)) return constant;
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("枚举值无法识别: " + value + " (期望: " + type.getSimpleName() + ")");
    }
}
