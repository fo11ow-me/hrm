package com.qiujie.util;

import com.qiujie.enums.TaskModuleEnum;

import java.util.*;

/**
 * Excel 列名 → Java 字段映射注册表。
 * <p>
 * 替代各 DTO 上的 {@code @ExcelProperty} 注解，集中管理映射关系。
 * 新增模块只需在这里加一个 Map，无需修改 DTO 或 Entity。
 *
 * @author qiujie
 */
public final class ColumnMappingRegistry {

    private ColumnMappingRegistry() {}

    /**
     * 模块 → (Excel表头中文 → 实体字段名)
     */
    private static final Map<TaskModuleEnum, Map<String, String>> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put(TaskModuleEnum.ATTENDANCE, Map.ofEntries(
                Map.entry("员工id", "staffId"),
                Map.entry("上午上班时间", "morStartTime"),
                Map.entry("上午下班时间", "morEndTime"),
                Map.entry("下午上班时间", "aftStartTime"),
                Map.entry("下午下班时间", "aftEndTime"),
                Map.entry("考勤日期", "attendanceDate")
        ));

        REGISTRY.put(TaskModuleEnum.STAFF_OVERTIME, Map.ofEntries(
                Map.entry("员工id", "staffId"),
                Map.entry("上午上班时间", "morStartTime"),
                Map.entry("上午下班时间", "morEndTime"),
                Map.entry("下午上班时间", "aftStartTime"),
                Map.entry("下午下班时间", "aftEndTime"),
                Map.entry("加班日期", "overtimeDate")
        ));

        REGISTRY.put(TaskModuleEnum.STAFF, Map.ofEntries(
                Map.entry("工号", "code"),
                Map.entry("姓名", "name"),
                Map.entry("地址", "address"),
                Map.entry("生日", "birthday"),
                Map.entry("电话", "phone"),
                Map.entry("备注", "remark"),
                Map.entry("部门id", "deptId"),
                Map.entry("创建时间", "createTime"),
                Map.entry("更新时间", "updateTime")
        ));

    }

    /**
     * 获取指定模块的列名映射（key = Excel表头中文, value = 实体字段名）。
     */
    public static Map<String, String> get(TaskModuleEnum module) {
        Map<String, String> mapping = REGISTRY.get(module);
        if (mapping == null) {
            return Collections.emptyMap();
        }
        return mapping;
    }

    /**
     * 已知表头文字，查询对应的字段名。
     *
     * @return 字段名，未命中返回 null
     */
    public static String resolve(TaskModuleEnum module, String excelHeader) {
        Map<String, String> mapping = REGISTRY.get(module);
        if (mapping == null) return null;
        return mapping.get(excelHeader.trim());
    }
}
