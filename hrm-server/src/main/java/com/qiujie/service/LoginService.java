package com.qiujie.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Staff;
import com.qiujie.entity.StaffDetails;
import com.qiujie.entity.ValidateCode;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.JwtUtil;
import com.qiujie.util.RedisUtil;
import com.qiujie.util.ValidateCodeUtil;
import com.qiujie.vo.StaffDeptVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author : qiujie
 * @Date : 2022/1/30
 */

@Service
public class LoginService extends ServiceImpl<StaffMapper, Staff> {

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisUtil redisUtil;

    public ResponseDTO login(Staff staff, String validateCode, HttpServletResponse response) {
        // 校验验证码——Redis key 自带 TTL，过期自动删除，无需单独存储过期时间
        Object codeObj = redisUtil.get("validate:code");
        if (codeObj == null) {
            return Response.error("验证码不存在或已过期！");
        }
        if (!codeObj.toString().equals(validateCode)) {
            return Response.error("验证码错误！");
        }
        // AuthenticationManager authenticationManager进行用户认证
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(staff.getCode(), staff.getPassword());
        Authentication authenticate = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        // 认证失败框架会抛出异常
        // 认证通过
        StaffDetails staffDetails = (StaffDetails) authenticate.getPrincipal();
        StaffDeptVO staffDeptVO = this.staffMapper.queryByCode(staffDetails.getUsername());
        // 将 staffId 和权限写入 JWT claims，后续请求无需查 DB
        String permissions = staffDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.joining(","));

        // Access Token —— 15 分钟有效期，每次 API 请求携带
        String accessToken = JwtUtil.generateAccessToken(
                JwtUtil.buildClaims(staffDeptVO.getId(), permissions),
                staffDetails);
        Cookie accessCookie = new Cookie("token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (JwtUtil.ACCESS_EXPIRATION / 1000));
        response.addCookie(accessCookie);

        // Refresh Token —— 7 天有效期，仅 /refresh 路径发送，用于续期
        String refreshToken = JwtUtil.generateRefreshToken(
                staffDeptVO.getId(), permissions, staffDetails);
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/refresh");
        refreshCookie.setMaxAge((int) (JwtUtil.REFRESH_EXPIRATION / 1000));
        response.addCookie(refreshCookie);

        return Response.success(staffDeptVO, null);
    }

    public void getValidateCode(HttpServletResponse response) throws IOException {
        ValidateCode validateCode = ValidateCodeUtil.generateValidateCode();
        // Redis key 自带过期时间，过期后自动删除
        redisUtil.set("validate:code", validateCode.getCode(), ValidateCodeUtil.expireIn);
        ImageIO.write(validateCode.getImage(), "jpeg", response.getOutputStream());
    }
}
