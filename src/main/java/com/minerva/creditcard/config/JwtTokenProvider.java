package com.minerva.creditcard.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token Provider
 * PCI-DSS §7: 访问控制 - 系统和应用程序的访问必须基于业务和安全需求
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        // Security: JWT secret must be provided via environment variable, no default allowed
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "FATAL: JWT secret not configured. Set jwt.secret environment variable. " +
                "Production deployment requires a secure secret (min 256 bits for HS256)."
            );
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Security: Enforce minimum key length for HS256
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "FATAL: JWT secret too short. HS256 requires at least 256 bits (32 bytes)."
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(String subject, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Token 提取 subject（用户ID）
     */
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 从 Token 提取角色
     */
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 验证 Token 有效性
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}