package com.qiujie.handler;

import com.alibaba.fastjson.JSON;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.util.WebUtil;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
public class AuthenticationEntryPointHandler implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        String str = JSON.toJSONString(new ResponseDTO(BusinessStatusEnum.UNAUTHORIZED));
        // 给出异常提示信息
        WebUtil.renderString(response, str);
    }
}
