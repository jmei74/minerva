package com.minerva.creditcard.controller;

import com.minerva.creditcard.config.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * PCI-DSS §8: 唯一 ID 认证
 *
 * 生产环境: 应连接真实用户数据库验证用户名/密码
 * 当前实现: 硬编码演示用户（供开发测试用）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 登录获取 JWT Token
     * POST /api/auth/login
     *
     * 生产环境应:
     * 1. 连接用户数据库验证密码（BCrypt 加密存储）
     * 2. 记录登录审计日志
     * 3. 失败超过 N 次后锁定账户
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // 演示用户（实际应查数据库）
        // TODO: 连接用户数据库验证
        if (!"admin".equals(request.username) || !"admin123".equals(request.password)) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Invalid username or password"
            ));
        }

        String token = jwtTokenProvider.generateToken(request.username, "ADMIN");

        return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "username", request.username,
                "expiresIn", 86400000
        ));
    }

    /**
     * 验证 Token
     * GET /api/auth/verify
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "No token provided"
            ));
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Invalid or expired token"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "subject", jwtTokenProvider.getSubject(token),
                "role", jwtTokenProvider.getRole(token),
                "valid", true
        ));
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }
}