package com.qiujie.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Menu;
import com.qiujie.entity.Staff;
import com.qiujie.mapper.MenuMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.service.LoginService;
import com.qiujie.util.JwtUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录注册接口
 *
 * @Author : qiujie
 * @Date : 2022/1/30
 */
@RestController
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private MenuMapper menuMapper;

    @PostMapping("/login/{validateCode}")
    public ResponseDTO login(@RequestBody Staff staff, @PathVariable String validateCode,
                             HttpServletResponse response) {
        return this.loginService.login(staff, validateCode, response);
    }

    @GetMapping("/validate/code")
    public void getValidateCode(HttpServletResponse response) throws IOException {
        this.loginService.getValidateCode(response);
    }

    @ApiOperation("刷新 Access Token")
    @PostMapping("/refresh")
    public ResponseDTO refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null) {
            return Response.error("Refresh Token 不存在");
        }
        try {
            String type = JwtUtil.extractTokenType(refreshToken);
            if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(type)) {
                return Response.error("非法 Token 类型");
            }
            if (JwtUtil.isTokenExpired(refreshToken)) {
                return Response.error("Refresh Token 已过期，请重新登录");
            }
            String username = JwtUtil.extractUsername(refreshToken);
            Integer staffId = JwtUtil.extractStaffId(refreshToken);

            // 重新查询员工状态，离职/禁用则拒绝续期
            Staff staff = staffMapper.selectOne(new QueryWrapper<Staff>()
                    .eq("code", username).eq("is_deleted", 0));
            if (staff == null || staff.getStatus() != 1) {
                return Response.error("用户状态异常，请重新登录");
            }

            // 重新查询最新权限，确保权限变更在 15 分钟内生效
            List<Menu> menus = menuMapper.queryPermission(staffId);
            String permissions = menus.stream()
                    .map(Menu::getPermission)
                    .collect(Collectors.joining(","));

            String newAccessToken = JwtUtil.generateAccessToken(staffId, permissions, username);
            Cookie accessCookie = new Cookie("token", newAccessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge((int) (JwtUtil.ACCESS_EXPIRATION / 1000));
            response.addCookie(accessCookie);
            return Response.success("Token 已刷新");
        } catch (Exception e) {
            return Response.error("Token 无效: " + e.getMessage());
        }
    }
}
