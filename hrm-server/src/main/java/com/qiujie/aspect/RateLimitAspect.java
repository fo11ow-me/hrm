package com.qiujie.aspect;

import com.qiujie.annotation.RateLimit;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.util.SecurityUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 速率限制切面
 *
 * @author qiujie
 * @date 2026-06-09
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private HttpServletRequest request;

    @Autowired(required = false)
    private SecurityUtil securityUtil;

    @Around("@annotation(com.qiujie.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        String key = buildKey(rateLimit);
        if (!StringUtils.hasText(key)) {
            return joinPoint.proceed();
        }

        // 获取当前请求次数
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);

        // 检查是否超过限制
        if (count >= rateLimit.value()) {
            log.warn("Rate limit exceeded: key={}, count={}", key, count);
            return Response.error("请求过于频繁，请稍后再试");
        }

        // 增加计数
        if (count == 0) {
            redisTemplate.opsForValue().set(key, "1", rateLimit.timeout(), TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(key);
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        String identifier;

        switch (rateLimit.type()) {
            case IP:
                identifier = getClientIp();
                break;
            case USER:
                identifier = getUserId();
                break;
            case GLOBAL:
                identifier = "global";
                break;
            default:
                return null;
        }

        if (!StringUtils.hasText(identifier)) {
            return null;
        }

        return RATE_LIMIT_KEY_PREFIX + rateLimit.type().name().toLowerCase() + ":" + identifier;
    }

    private String getClientIp() {
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果有多个 IP,取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    private String getUserId() {
        if (securityUtil == null) {
            return null;
        }

        Integer userId = securityUtil.getCurrentOperatorId();
        return userId == null ? null : userId.toString();
    }
}
