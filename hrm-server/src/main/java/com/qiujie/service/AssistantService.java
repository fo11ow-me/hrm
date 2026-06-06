package com.qiujie.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.assistant.AssistantLlmClient;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.dto.assistant.AssistantChatRequest;
import com.qiujie.dto.assistant.AssistantChatResponse;
import com.qiujie.dto.assistant.AssistantReference;
import com.qiujie.entity.AssistantConversation;
import com.qiujie.entity.AssistantMessage;
import com.qiujie.entity.AssistantToolCall;
import com.qiujie.entity.Salary;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import com.qiujie.mapper.AssistantConversationMapper;
import com.qiujie.mapper.AssistantMessageMapper;
import com.qiujie.mapper.AssistantToolCallMapper;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.SalaryMapper;
import com.qiujie.mapper.StaffLeaveMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.mapper.StaffOvertimeMapper;
import com.qiujie.util.SecurityUtil;
import com.qiujie.vo.AttendanceMonthSummaryVO;
import com.qiujie.vo.StaffDeptVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private static final String DEFAULT_SCENE = "employee_self_service";

    @Autowired
    private AssistantConversationMapper conversationMapper;

    @Autowired
    private AssistantMessageMapper messageMapper;

    @Autowired
    private AssistantToolCallMapper toolCallMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private StaffLeaveMapper staffLeaveMapper;

    @Autowired
    private StaffOvertimeMapper staffOvertimeMapper;

    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired(required = false)
    private AssistantLlmClient llmClient;

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO chat(AssistantChatRequest request) {
        String question = request == null ? null : request.getMessage();
        if (!StringUtils.hasText(question)) {
            return Response.error("问题不能为空");
        }
        if (question.length() > 1000) {
            return Response.error("问题长度不能超过1000个字符");
        }

        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            return Response.error(BusinessStatusEnum.UNAUTHORIZED);
        }

        String intent = detectIntent(question);
        AssistantConversation conversation = getOrCreateConversation(request, staffId, question);
        AssistantMessage userMessage = saveMessage(conversation.getId(), staffId, "USER", question, intent, null);

        ToolResult toolResult = executeTool(intent, question, staffId, conversation.getId(), userMessage.getId());
        String answer = buildAnswer(question, toolResult);
        AssistantMessage assistantMessage = saveMessage(conversation.getId(), staffId, "ASSISTANT", answer,
                toolResult.intent, JSON.toJSONString(toolResult.references));
        updateConversation(conversation);

        AssistantChatResponse response = new AssistantChatResponse()
                .setConversationId(conversation.getId())
                .setIntent(toolResult.intent)
                .setAnswer(answer)
                .setReferences(toolResult.references)
                .setSuggestions(buildSuggestions(toolResult.intent));
        return Response.success(response);
    }

    public ResponseDTO listConversations() {
        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            return Response.error(BusinessStatusEnum.UNAUTHORIZED);
        }
        List<AssistantConversation> list = conversationMapper.selectList(new QueryWrapper<AssistantConversation>()
                .eq("staff_id", staffId)
                .orderByDesc("update_time"));
        return Response.success(list);
    }

    public ResponseDTO queryConversation(Long id) {
        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            return Response.error(BusinessStatusEnum.UNAUTHORIZED);
        }
        AssistantConversation conversation = conversationMapper.selectById(id);
        if (conversation == null || !staffId.equals(conversation.getStaffId())) {
            return Response.error("会话不存在或无权访问");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("conversation", conversation);
        data.put("messages", messageMapper.selectList(new QueryWrapper<AssistantMessage>()
                .eq("conversation_id", id)
                .eq("staff_id", staffId)
                .orderByAsc("create_time")));
        return Response.success(data);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteConversation(Long id) {
        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            return Response.error(BusinessStatusEnum.UNAUTHORIZED);
        }
        AssistantConversation conversation = conversationMapper.selectById(id);
        if (conversation == null || !staffId.equals(conversation.getStaffId())) {
            return Response.error("会话不存在或无权删除");
        }
        conversationMapper.deleteById(id);
        messageMapper.delete(new QueryWrapper<AssistantMessage>().eq("conversation_id", id).eq("staff_id", staffId));
        toolCallMapper.delete(new QueryWrapper<AssistantToolCall>().eq("conversation_id", id).eq("staff_id", staffId));
        return Response.success();
    }

    private AssistantConversation getOrCreateConversation(AssistantChatRequest request, Integer staffId, String question) {
        Long conversationId = request == null ? null : request.getConversationId();
        if (conversationId != null) {
            AssistantConversation existing = conversationMapper.selectById(conversationId);
            if (existing != null && staffId.equals(existing.getStaffId())) {
                return existing;
            }
        }
        AssistantConversation conversation = new AssistantConversation()
                .setStaffId(staffId)
                .setTitle(buildTitle(question))
                .setScene(request == null || !StringUtils.hasText(request.getScene()) ? DEFAULT_SCENE : request.getScene());
        conversationMapper.insert(conversation);
        return conversation;
    }

    private AssistantMessage saveMessage(Long conversationId, Integer staffId, String role,
                                         String content, String intent, String referencesJson) {
        AssistantMessage message = new AssistantMessage()
                .setConversationId(conversationId)
                .setStaffId(staffId)
                .setRole(role)
                .setContent(content)
                .setIntent(intent)
                .setReferencesJson(referencesJson);
        messageMapper.insert(message);
        return message;
    }

    private void updateConversation(AssistantConversation conversation) {
        if (conversation.getId() == null) {
            return;
        }
        conversationMapper.updateById(new AssistantConversation().setId(conversation.getId())
                .setUpdateTime(new Timestamp(System.currentTimeMillis())));
    }

    private ToolResult executeTool(String intent, String question, Integer staffId, Long conversationId, Long messageId) {
        ToolResult result;
        try {
            switch (intent) {
                case "FORBIDDEN":
                    result = ToolResult.forbidden("员工自助助手只能查询你本人可见的人事数据，暂不支持查询其他员工、部门统计或全公司数据。");
                    break;
                case "ATTENDANCE":
                    result = queryMyAttendance(question, staffId);
                    break;
                case "LEAVE":
                    result = queryMyLeave(staffId);
                    break;
                case "OVERTIME":
                    result = queryMyOvertime(question, staffId);
                    break;
                case "SALARY":
                    result = queryMySalary(question, staffId);
                    break;
                case "PROFILE":
                    result = queryMyProfile(staffId);
                    break;
                case "SYSTEM_HELP":
                    result = querySystemHelp(question);
                    break;
                default:
                    result = ToolResult.unknown("我可以帮你查询本人的考勤、请假/加班、薪资摘要、个人档案，也可以说明导入导出和审批操作。");
                    break;
            }
            saveToolCall(conversationId, messageId, staffId, result, null);
            return result;
        } catch (Exception e) {
            log.warn("Assistant tool failed, intent={}, staffId={}", intent, staffId, e);
            result = ToolResult.unknown("查询过程中出现异常，请稍后再试，或进入对应业务页面查看。");
            result.intent = intent;
            saveToolCall(conversationId, messageId, staffId, result, e.getMessage());
            return result;
        }
    }

    private ToolResult queryMyAttendance(String question, Integer staffId) {
        String month = extractMonth(question);
        Collection<Integer> staffIds = Collections.singletonList(staffId);
        List<AttendanceMonthSummaryVO> summaries = attendanceMapper.queryMonthSummaryByStaffIds(month, staffIds);
        AttendanceMonthSummaryVO summary = summaries == null || summaries.isEmpty() ? new AttendanceMonthSummaryVO() : summaries.get(0);
        Map<String, Object> data = new HashMap<>();
        data.put("month", month);
        data.put("lateTimes", valueOrZero(summary.getLateTimes()));
        data.put("leaveEarlyTimes", valueOrZero(summary.getLeaveEarlyTimes()));
        data.put("absenteeismTimes", valueOrZero(summary.getAbsenteeismTimes()));
        data.put("leaveDays", valueOrZero(summary.getLeaveDays()));
        data.put("timeOffDays", valueOrZero(summary.getTimeOffDays()));
        String fallback = String.format("%s 的考勤摘要：迟到 %d 次，早退 %d 次，旷工 %d 次，请假 %d 天，调休 %d 天。",
                formatMonth(month),
                data.get("lateTimes"),
                data.get("leaveEarlyTimes"),
                data.get("absenteeismTimes"),
                data.get("leaveDays"),
                data.get("timeOffDays"));
        return ToolResult.success("ATTENDANCE", "queryMyAttendanceSummary", data, fallback,
                new AssistantReference("attendance", "我的考勤摘要", JSON.toJSONString(data)));
    }

    private ToolResult queryMyLeave(Integer staffId) {
        List<StaffLeave> records = staffLeaveMapper.selectList(new QueryWrapper<StaffLeave>()
                .eq("staff_id", staffId)
                .orderByDesc("create_time")
                .last("limit 5"));
        if (records == null) {
            records = new ArrayList<>();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("recentCount", records.size());
        data.put("records", records);
        String fallback = records.isEmpty()
                ? "你最近没有请假申请记录。"
                : "已为你查询最近 " + records.size() + " 条请假申请，建议在请假记录页查看完整审批进度。";
        return ToolResult.success("LEAVE", "queryMyLeaveStatus", data, fallback,
                new AssistantReference("leave", "我的请假记录", "最近 " + records.size() + " 条"));
    }

    private ToolResult queryMyOvertime(String question, Integer staffId) {
        String month = extractMonth(question);
        Integer overtimeTimes = staffOvertimeMapper.countTimes(staffId, OvertimeStatusEnum.OVERTIME.getCode(), month);
        Integer timeOffDays = staffOvertimeMapper.countTimes(staffId, OvertimeStatusEnum.TIME_OFF.getCode(), month);
        BigDecimal overtimeSalary = staffOvertimeMapper.sumMonthOvertimeSalary(staffId, month);
        Map<String, Object> data = new HashMap<>();
        data.put("month", month);
        data.put("overtimeTimes", valueOrZero(overtimeTimes));
        data.put("timeOffDays", valueOrZero(timeOffDays));
        data.put("overtimeSalary", moneyOrZero(overtimeSalary));
        String fallback = String.format("%s 的加班摘要：加班 %d 次，调休 %d 天，加班工资合计 %s 元。",
                formatMonth(month), data.get("overtimeTimes"), data.get("timeOffDays"), data.get("overtimeSalary"));
        return ToolResult.success("OVERTIME", "queryMyOvertimeSummary", data, fallback,
                new AssistantReference("overtime", "我的加班摘要", JSON.toJSONString(data)));
    }

    private ToolResult queryMySalary(String question, Integer staffId) {
        String month = extractMonth(question);
        Salary salary = salaryMapper.selectOne(new QueryWrapper<Salary>()
                .eq("staff_id", staffId)
                .eq("month", month)
                .last("limit 1"));
        Map<String, Object> data = new HashMap<>();
        data.put("month", month);
        if (salary == null) {
            String fallback = formatMonth(month) + " 暂未查询到你的薪资记录。";
            return ToolResult.success("SALARY", "queryMySalarySummary", data, fallback,
                    new AssistantReference("salary", "我的薪资摘要", fallback));
        }
        BigDecimal totalDeduct = moneyOrZero(salary.getLateDeduct())
                .add(moneyOrZero(salary.getLeaveDeduct()))
                .add(moneyOrZero(salary.getLeaveEarlyDeduct()))
                .add(moneyOrZero(salary.getAbsenteeismDeduct()));
        data.put("totalSalary", moneyOrZero(salary.getTotalSalary()));
        data.put("overtimeSalary", moneyOrZero(salary.getOvertimeSalary()));
        data.put("attendanceDeduct", totalDeduct);
        String fallback = String.format("%s 的薪资摘要：合计工资 %s 元，加班工资 %s 元，考勤相关扣款 %s 元。",
                formatMonth(month), data.get("totalSalary"), data.get("overtimeSalary"), data.get("attendanceDeduct"));
        return ToolResult.success("SALARY", "queryMySalarySummary", data, fallback,
                new AssistantReference("salary", "我的薪资摘要", JSON.toJSONString(data)));
    }

    private ToolResult queryMyProfile(Integer staffId) {
        StaffDeptVO staff = staffMapper.queryInfo(staffId);
        if (staff == null) {
            return ToolResult.success("PROFILE", "queryMyProfile", Collections.emptyMap(),
                    "暂未查询到你的档案信息，请联系管理员确认账号状态。",
                    new AssistantReference("profile", "我的档案", "未查询到档案"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", staff.getId());
        data.put("code", staff.getCode());
        data.put("name", staff.getName());
        data.put("deptName", staff.getDeptName());
        data.put("phone", maskPhone(staff.getPhone()));
        data.put("status", staff.getStatus());
        String fallback = String.format("你的档案信息：姓名 %s，工号 %s，部门 %s，联系电话 %s。",
                emptyToDash(staff.getName()), emptyToDash(staff.getCode()), emptyToDash(staff.getDeptName()), emptyToDash(maskPhone(staff.getPhone())));
        return ToolResult.success("PROFILE", "queryMyProfile", data, fallback,
                new AssistantReference("profile", "我的档案", JSON.toJSONString(data)));
    }

    private ToolResult querySystemHelp(String question) {
        String answer;
        if (containsAny(question, "导入", "上传", "Excel", "excel")) {
            answer = "考勤导入可以进入“绩效管理 -> 考勤管理”，点击导入按钮上传 xlsx 文件。大批量考勤建议使用异步导入任务，提交后可在文件任务面板查看进度、错误明细和结果文件。";
        } else if (containsAny(question, "导出", "下载")) {
            answer = "导出数据时进入对应业务页面点击导出按钮。考勤大批量导出会创建后台任务，完成后可在文件任务面板下载结果文件。";
        } else if (containsAny(question, "审批", "请假", "流程")) {
            answer = "请假申请可在右上角个人菜单提交，审批记录可在请假记录页查看。审批人会在绩效管理的请假审批列表处理任务。";
        } else {
            answer = "你可以问我：我的考勤、我的请假、我的加班、我的薪资摘要、我的档案，以及导入导出或审批怎么操作。";
        }
        ToolResult result = ToolResult.success("SYSTEM_HELP", "querySystemHelp", Collections.singletonMap("question", question), answer,
                new AssistantReference("help", "系统使用说明", answer));
        result.allowLlm = false;
        return result;
    }

    private String buildAnswer(String question, ToolResult toolResult) {
        if (!toolResult.allowLlm || llmClient == null) {
            return toolResult.fallbackAnswer;
        }
        try {
            String modelAnswer = llmClient.generate(question, JSON.toJSONString(toolResult.data));
            if (StringUtils.hasText(modelAnswer)) {
                return modelAnswer;
            }
        } catch (Exception e) {
            log.warn("Assistant LLM failed, fallback to tool answer", e);
        }
        return toolResult.fallbackAnswer;
    }

    private void saveToolCall(Long conversationId, Long messageId, Integer staffId, ToolResult result, String errorMessage) {
        AssistantToolCall toolCall = new AssistantToolCall()
                .setConversationId(conversationId)
                .setMessageId(messageId)
                .setStaffId(staffId)
                .setIntent(result.intent)
                .setToolName(result.toolName)
                .setArgumentsJson(JSON.toJSONString(result.arguments))
                .setResultJson(JSON.toJSONString(result.data))
                .setStatus(errorMessage == null ? "SUCCESS" : "FAILED")
                .setErrorMessage(errorMessage);
        toolCallMapper.insert(toolCall);
    }

    private String detectIntent(String question) {
        if (isForbiddenQuestion(question)) {
            return "FORBIDDEN";
        }
        if (containsAny(question, "怎么", "如何", "帮助", "导入", "导出", "上传", "下载", "审批", "菜单")) {
            return "SYSTEM_HELP";
        }
        if (containsAny(question, "考勤", "迟到", "早退", "旷工", "出勤")) {
            return "ATTENDANCE";
        }
        if (containsAny(question, "请假", "休假", "假期")) {
            return "LEAVE";
        }
        if (containsAny(question, "加班", "调休")) {
            return "OVERTIME";
        }
        if (containsAny(question, "工资", "薪资", "薪水")) {
            return "SALARY";
        }
        if (containsAny(question, "档案", "个人信息", "我是谁", "我的信息")) {
            return "PROFILE";
        }
        return "UNKNOWN";
    }

    private boolean isForbiddenQuestion(String question) {
        boolean asksOtherPeople = containsAny(question, "张三", "李四", "王五", "其他员工", "别人", "他人", "某个员工");
        boolean asksAggregate = containsAny(question, "全公司", "所有人", "全部员工", "部门统计", "部门考勤", "部门薪资", "排名");
        boolean ownScope = containsAny(question, "我", "我的", "本人", "自己");
        return asksOtherPeople || (asksAggregate && !ownScope);
    }

    private List<String> buildSuggestions(String intent) {
        List<String> suggestions = new ArrayList<>();
        switch (intent) {
            case "ATTENDANCE":
                suggestions.add("查看我的加班情况");
                suggestions.add("怎么导出考勤");
                break;
            case "SALARY":
                suggestions.add("查看我的考勤扣款");
                suggestions.add("查看我的加班工资");
                break;
            case "PROFILE":
                suggestions.add("我的考勤情况");
                suggestions.add("我的请假记录");
                break;
            default:
                suggestions.add("我的考勤");
                suggestions.add("我的请假记录");
                suggestions.add("我的薪资摘要");
                break;
        }
        return suggestions;
    }

    private String extractMonth(String question) {
        Matcher compact = Pattern.compile("(20\\d{2})(0[1-9]|1[0-2])").matcher(question);
        if (compact.find()) {
            return compact.group(1) + compact.group(2);
        }
        Matcher cn = Pattern.compile("(20\\d{2})\\s*年\\s*(1[0-2]|0?[1-9])\\s*月?").matcher(question);
        if (cn.find()) {
            return cn.group(1) + String.format("%02d", Integer.parseInt(cn.group(2)));
        }
        Matcher shortMonth = Pattern.compile("(?<!\\d)(1[0-2]|0?[1-9])\\s*月").matcher(question);
        if (shortMonth.find()) {
            YearMonth now = YearMonth.now();
            return now.getYear() + String.format("%02d", Integer.parseInt(shortMonth.group(1)));
        }
        return YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private String formatMonth(String month) {
        if (month == null || month.length() != 6) {
            return month;
        }
        return month.substring(0, 4) + "年" + Integer.parseInt(month.substring(4)) + "月";
    }

    private String buildTitle(String question) {
        String compact = question.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 20) {
            return compact;
        }
        return compact.substring(0, 20);
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    private String emptyToDash(String value) {
        return StrUtil.isBlank(value) ? "-" : value;
    }

    private static class ToolResult {
        private String intent;
        private String toolName;
        private Map<String, Object> arguments = new HashMap<>();
        private Object data;
        private String fallbackAnswer;
        private boolean allowLlm = true;
        private List<AssistantReference> references = new ArrayList<>();

        private static ToolResult success(String intent, String toolName, Object data,
                                          String fallbackAnswer, AssistantReference reference) {
            ToolResult result = new ToolResult();
            result.intent = intent;
            result.toolName = toolName;
            result.data = data;
            result.fallbackAnswer = fallbackAnswer;
            result.references.add(reference);
            return result;
        }

        private static ToolResult forbidden(String fallbackAnswer) {
            ToolResult result = success("FORBIDDEN", "rejectOutOfScope", Collections.emptyMap(), fallbackAnswer,
                    new AssistantReference("security", "权限边界", fallbackAnswer));
            result.allowLlm = false;
            return result;
        }

        private static ToolResult unknown(String fallbackAnswer) {
            ToolResult result = success("UNKNOWN", "clarifyQuestion", Collections.emptyMap(), fallbackAnswer,
                    new AssistantReference("assistant", "能力范围", fallbackAnswer));
            result.allowLlm = false;
            return result;
        }
    }
}
