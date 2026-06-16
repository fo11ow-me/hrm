package com.qiujie.security;

import com.qiujie.controller.StaffLeaveController;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.StaffLeaveService;
import com.qiujie.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 @PreAuthorize 拦截的是 Controller Bean 的方法调用，不是 HTTP 请求。
 * 绕过 HTTP 直接调 Controller 方法，@PreAuthorize 照常生效。
 */
@SpringBootTest(properties = {
    "spring.profiles.active=dev",
    "server.port=0",
    "assistant.enabled=false",
    "knowledge.enabled=false"
})
class PreAuthorizeDirectCallTest {

    @Autowired
    private StaffLeaveController leaveController;

    @BeforeEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 有 leave:list 权限 → 方法调用成功
     */
    @Test
    void shouldAllowWhenAuthorized() {
        setAuthority("performance:leave:list");

        // 直接调 Controller 方法，不经过 HTTP
        ResponseDTO result = leaveController.list(1, 5, null, null, null);

        assertNotNull(result);
        assertEquals(200, result.getCode(), "有权限时应返回 200");
    }

    /**
     * 只有无关权限 → AccessDeniedException
     */
    @Test
    void shouldDenyWhenUnauthorized() {
        setAuthority("some:random:permission");

        assertThrows(AccessDeniedException.class, () -> {
            // 同上，直接调——@PreAuthorize 通过 AOP 代理拦截
            leaveController.list(1, 5, null, null, null);
        }, "无 leave:list 权限应抛 AccessDeniedException");
    }

    private void setAuthority(String auth) {
        var granted = List.of(new SimpleGrantedAuthority(auth));
        var authToken = UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, granted);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
