package com.qiujie.filter;

import com.qiujie.entity.StaffDetails;
import com.qiujie.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器——从请求中提取 token，验签后直接构建 SecurityContext。
 *
 * 核心优化：不再调用 StaffDetailsService.loadUserByUsername() 查 DB，
 * 而是从 JWT claims 中读取 staffId + permissions 直接构建认证对象。
 * 每个请求减少 2~3 次 DB 查询。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 三层回退获取 token: Header（正常请求）→ Query Param（SSE）→ Cookie（httpOnly）
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            String tokenParam = request.getParameter("token");
            if (StringUtils.hasText(tokenParam)) {
                authorization = "Bearer " + tokenParam;
            }
        }
        if (!StringUtils.hasText(authorization)) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                        authorization = "Bearer " + cookie.getValue();
                        break;
                    }
                }
            }
        }
        String token = null;
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7).trim();
        }

        if (StringUtils.hasText(token)) {
            try {
                // Refresh Token 不能用于访问 API
                if (!JwtUtil.TOKEN_TYPE_ACCESS.equals(JwtUtil.extractTokenType(token))) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String username = JwtUtil.extractUsername(token);
                if (StringUtils.hasText(username)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 从 JWT claims 直接构建认证信息，无需查 DB
                    List<SimpleGrantedAuthority> authorities = JwtUtil.extractPermissions(token).stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    StaffDetails staffDetails = new StaffDetails(
                            username, "", authorities, true, true, true, true);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            staffDetails, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                this.logger.warn("JWT 解析失败: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}