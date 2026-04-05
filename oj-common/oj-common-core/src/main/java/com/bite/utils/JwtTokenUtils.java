package com.bite.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类：
 * 1) 仅生成/校验令牌，不在 JWT 内存放业务用户信息；
 * 2) 用户真实信息统一放到 Redis。
 */
public final class JwtTokenUtils {

    private JwtTokenUtils() {
    }

    public static String generateToken(String secret, long expireSeconds) {
        long now = System.currentTimeMillis();
        Date issueAt = new Date(now);
        Date expireAt = new Date(now + expireSeconds * 1000L);
        String jti = UUID.randomUUID().toString().replace("-", "");
        return Jwts.builder()
                .setId(jti)
                .setIssuedAt(issueAt)
                .setExpiration(expireAt)
                .signWith(buildKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseClaims(String token, String secret) {
        return Jwts.parserBuilder()
                .setSigningKey(buildKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static boolean validateToken(String token, String secret) {
        try {
            parseClaims(token, secret);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static SecretKey buildKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }
}
