package com.qiujie.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static Key signingKey;
    // Access Token 15 分钟过期，Refresh Token 7 天过期
    public final static long ACCESS_EXPIRATION = 15 * 60 * 1000;
    public final static long REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    public JwtUtil(@Value("${jwt.secret}") String secretKey) {
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    private static Key getSignInKey() {
        if (signingKey == null) {
            throw new IllegalStateException("JWT signing key is not configured");
        }
        return signingKey;
    }

    private static Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public static Integer extractStaffId(String token) {
        return extractClaim(token, claims -> claims.get("staffId", Integer.class));
    }

    public static List<String> extractPermissions(String token) {
        String permissionsStr = extractClaim(token, claims -> claims.get("permissions", String.class));
        if (permissionsStr == null || permissionsStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(permissionsStr.split(","));
    }

    public static String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private static Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private static String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Map<String, Object> buildClaims(Integer staffId, String permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("staffId", staffId);
        claims.put("permissions", permissions);
        return claims;
    }

    // 生成 Access Token（15 分钟，每次 API 请求携带）
    public static String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        extraClaims.put("type", TOKEN_TYPE_ACCESS);
        return buildToken(extraClaims, userDetails, ACCESS_EXPIRATION);
    }

    // 按用户名生成 Access Token（续期时无需 UserDetails 对象）
    public static String generateAccessToken(Integer staffId, String permissions, String username) {
        Map<String, Object> claims = buildClaims(staffId, permissions);
        claims.put("type", TOKEN_TYPE_ACCESS);
        return buildToken(claims, createUser(username), ACCESS_EXPIRATION);
    }

    // 生成 Refresh Token（7 天，仅用于 /refresh 接口续期）
    public static String generateRefreshToken(Integer staffId, String permissions, UserDetails userDetails) {
        Map<String, Object> claims = buildClaims(staffId, permissions);
        claims.put("type", TOKEN_TYPE_REFRESH);
        return buildToken(claims, userDetails, REFRESH_EXPIRATION);
    }

    private static UserDetails createUser(String username) {
        return new org.springframework.security.core.userdetails.User(
                username, "", new ArrayList<>());
    }

    public static boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public static boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
