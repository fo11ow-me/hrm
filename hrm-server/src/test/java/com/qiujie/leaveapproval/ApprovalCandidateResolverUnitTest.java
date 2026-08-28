package com.qiujie.leaveapproval;

import com.qiujie.entity.Staff;
import com.qiujie.mapper.StaffMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批候选人解析单测——零 Spring 上下文，mock StaffMapper。
 */
@DisplayName("审批候选人解析")
class ApprovalCandidateResolverUnitTest {

    private StaffMapper staffMapper;
    private ApprovalCandidateResolver resolver;

    @BeforeEach
    void setUp() {
        staffMapper = mock(StaffMapper.class);
        resolver = new ApprovalCandidateResolver(staffMapper);
    }

    @Test
    @DisplayName("角色成员 → 工号逗号串")
    void codesOfRole_ShouldJoinCodes() {
        Staff m1 = new Staff();
        m1.setCode("A001");
        Staff m2 = new Staff();
        m2.setCode("A002");
        when(staffMapper.queryByRole("manager")).thenReturn(List.of(m1, m2));

        String codes = resolver.codesOfRole("manager");

        assertEquals("A001,A002", codes);
        verify(staffMapper).queryByRole("manager");
    }

    @Test
    @DisplayName("角色无成员 → 空串")
    void codesOfRole_Empty_ShouldReturnEmpty() {
        when(staffMapper.queryByRole("manager")).thenReturn(List.of());

        assertEquals("", resolver.codesOfRole("manager"));
    }
}