package com.qiujie.service;

import com.qiujie.assistant.AssistantLlmClient;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.dto.assistant.AssistantChatRequest;
import com.qiujie.dto.assistant.AssistantChatResponse;
import com.qiujie.entity.AssistantConversation;
import com.qiujie.entity.AssistantMessage;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceUnitTest {

    @Mock
    private AssistantConversationMapper conversationMapper;

    @Mock
    private AssistantMessageMapper messageMapper;

    @Mock
    private AssistantToolCallMapper toolCallMapper;

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private StaffMapper staffMapper;

    @Mock
    private StaffLeaveMapper staffLeaveMapper;

    @Mock
    private StaffOvertimeMapper staffOvertimeMapper;

    @Mock
    private SalaryMapper salaryMapper;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private AssistantLlmClient llmClient;

    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        assistantService = new AssistantService();
        ReflectionTestUtils.setField(assistantService, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(assistantService, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(assistantService, "toolCallMapper", toolCallMapper);
        ReflectionTestUtils.setField(assistantService, "attendanceMapper", attendanceMapper);
        ReflectionTestUtils.setField(assistantService, "staffMapper", staffMapper);
        ReflectionTestUtils.setField(assistantService, "staffLeaveMapper", staffLeaveMapper);
        ReflectionTestUtils.setField(assistantService, "staffOvertimeMapper", staffOvertimeMapper);
        ReflectionTestUtils.setField(assistantService, "salaryMapper", salaryMapper);
        ReflectionTestUtils.setField(assistantService, "securityUtil", securityUtil);
        ReflectionTestUtils.setField(assistantService, "llmClient", llmClient);
    }

    @Test
    void chat_AttendanceQuestion_ShouldQueryCurrentStaffOnly() {
        when(securityUtil.getCurrentOperatorId()).thenReturn(7);
        AttendanceMonthSummaryVO summary = new AttendanceMonthSummaryVO();
        summary.setStaffId(7);
        summary.setLateTimes(2);
        summary.setLeaveEarlyTimes(1);
        summary.setAbsenteeismTimes(0);
        summary.setLeaveDays(3);
        summary.setTimeOffDays(1);
        when(attendanceMapper.queryMonthSummaryByStaffIds(eq("202606"), any(Collection.class)))
                .thenReturn(Collections.singletonList(summary));
        when(llmClient.generate(anyString(), anyString())).thenReturn("本月迟到 2 次，早退 1 次，请假 3 天。");

        ResponseDTO result = assistantService.chat(new AssistantChatRequest()
                .setMessage("帮我查一下2026年6月考勤"));

        assertThat(result.getCode()).isEqualTo(200);
        AssistantChatResponse data = (AssistantChatResponse) result.getData();
        assertThat(data.getIntent()).isEqualTo("ATTENDANCE");
        assertThat(data.getAnswer()).contains("迟到 2 次");

        ArgumentCaptor<Collection<Integer>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(attendanceMapper).queryMonthSummaryByStaffIds(eq("202606"), captor.capture());
        assertThat(captor.getValue()).containsExactly(7);
    }

    @Test
    void chat_CrossEmployeeSalaryQuestion_ShouldRejectWithoutToolOrLlmCall() {
        when(securityUtil.getCurrentOperatorId()).thenReturn(7);

        ResponseDTO result = assistantService.chat(new AssistantChatRequest()
                .setMessage("帮我查询张三的工资"));

        assertThat(result.getCode()).isEqualTo(200);
        AssistantChatResponse data = (AssistantChatResponse) result.getData();
        assertThat(data.getIntent()).isEqualTo("FORBIDDEN");
        assertThat(data.getAnswer()).contains("只能查询你本人");
        verify(salaryMapper, never()).selectOne(any());
        verify(llmClient, never()).generate(anyString(), anyString());
    }

    @Test
    void chat_ProfileQuestion_ShouldFallbackWhenLlmFails() {
        when(securityUtil.getCurrentOperatorId()).thenReturn(7);
        StaffDeptVO staff = new StaffDeptVO()
                .setId(7)
                .setCode("staff_7")
                .setName("李四")
                .setDeptName("技术部");
        when(staffMapper.queryInfo(7)).thenReturn(staff);
        when(llmClient.generate(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));

        ResponseDTO result = assistantService.chat(new AssistantChatRequest()
                .setMessage("我的档案信息"));

        assertThat(result.getCode()).isEqualTo(200);
        AssistantChatResponse data = (AssistantChatResponse) result.getData();
        assertThat(data.getIntent()).isEqualTo("PROFILE");
        assertThat(data.getAnswer()).contains("李四").contains("技术部");
    }

    @Test
    void chat_SystemHelpQuestion_ShouldReturnGuideWithoutLlm() {
        when(securityUtil.getCurrentOperatorId()).thenReturn(7);

        ResponseDTO result = assistantService.chat(new AssistantChatRequest()
                .setMessage("怎么导入考勤数据"));

        assertThat(result.getCode()).isEqualTo(200);
        AssistantChatResponse data = (AssistantChatResponse) result.getData();
        assertThat(data.getIntent()).isEqualTo("SYSTEM_HELP");
        assertThat(data.getAnswer()).contains("考勤").contains("导入");
        verify(llmClient, never()).generate(anyString(), anyString());
    }

    @Test
    void chat_ShouldPersistConversationAndMessages() {
        when(securityUtil.getCurrentOperatorId()).thenReturn(7);
        when(staffMapper.queryInfo(7)).thenReturn(new StaffDeptVO().setId(7).setName("李四"));
        when(llmClient.generate(anyString(), anyString())).thenReturn("你好，李四。");

        ResponseDTO result = assistantService.chat(new AssistantChatRequest()
                .setMessage("我是谁"));

        assertThat(result.getCode()).isEqualTo(200);
        verify(conversationMapper).insert(any(AssistantConversation.class));
        verify(messageMapper, times(2)).insert(any(AssistantMessage.class));
    }
}
