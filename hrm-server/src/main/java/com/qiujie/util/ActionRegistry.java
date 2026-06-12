package com.qiujie.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作注册表：定义助手可执行的操作及其所需权限。
 *
 * @author qiujie
 */
public final class ActionRegistry {

    private ActionRegistry() {}

    public static final List<ActionDef> ACTIONS = List.of(
        new ActionDef("LEAVE_APPLY",  "请假申请",   "/staff-leave",          "POST", null),
        new ActionDef("LEAVE_CANCEL", "撤销请假",   "/staff-leave",          "PUT",  null),
        new ActionDef("PROFILE_EDIT", "修改个人信息", "/staff",               "PUT",  null),
        new ActionDef("OVERTIME_SET", "设置加班",   "/staff-overtime/set",   "POST", "performance:overtime:set")
    );

    public static List<ActionDef> getAvailable(Collection<String> permissions) {
        if (permissions == null) permissions = Collections.emptySet();
        final Set<String> perms = new HashSet<>(permissions);
        return ACTIONS.stream()
            .filter(a -> a.requiredPermission == null || perms.contains(a.requiredPermission))
            .collect(Collectors.toList());
    }

    public static ActionDef lookup(String type) {
        return ACTIONS.stream().filter(a -> a.type.equals(type)).findFirst().orElse(null);
    }

    public static class ActionDef {
        public final String type;
        public final String label;
        public final String apiUrl;
        public final String apiMethod;
        public final String requiredPermission;

        ActionDef(String type, String label, String apiUrl, String apiMethod, String requiredPermission) {
            this.type = type;
            this.label = label;
            this.apiUrl = apiUrl;
            this.apiMethod = apiMethod;
            this.requiredPermission = requiredPermission;
        }
    }
}
