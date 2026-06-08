package com.minerva.creditcard.config;

import com.minerva.creditcard.util.CardEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 安全配置
 * 架构约束: §11 PCI-DSS - AES-256卡号加密 + §7/§8 访问控制
 *
 * JWT 鉴权策略:
 * - 所有 /api/** 请求需要有效 JWT Token
 * - /api/auth/** 用于获取 Token（登录）
 * - /actuator/health 公开访问（健康检查）
 * - 使用无状态 Session（不存储会话）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${aws.kms.key-id:}")
    private String kmsKeyId;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Bean
    public CardEncryptionUtil cardEncryptionUtil() {
        byte[] keyBytes = deriveKey(kmsKeyId);
        return new CardEncryptionUtil(keyBytes);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                // 禁用 CSRF（JWT 无状态，不需要）
                .csrf(AbstractHttpConfigurer::disable)

                // 无状态会话
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开端点
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        // 开发环境临时开放文档
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // SkyEye 静态面板
                        .requestMatchers("/skyeye.html", "/static/**").permitAll()

                        // Journey 监控数据（需要认证）
                        .requestMatchers("/api/v1/metrics/**").authenticated()
                        .requestMatchers("/api/**").authenticated()

                        // 其他全部拒绝
                        .anyRequest().denyAll())

                // 添加 JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前）
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // CORS（允许本地 file:// 页面访问 Dashboard）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 异常处理
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\":\"UNAUTHORIZED\",\"message\":\"Valid JWT token required\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\":\"FORBIDDEN\",\"message\":\"Insufficient permissions\"}"
                            );
                        }));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("https://localhost:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private byte[] deriveKey(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException(
                "FATAL: AWS KMS key ID not configured. Set aws.kms.key-id environment variable. " +
                "Card encryption requires keys from AWS KMS - hardcoded fallback is not permitted."
            );
        }
        // TODO: Replace with actual AWS KMS decryption call:
        // byte[] raw = kmsClient.decrypt(keyId);
        // For now, derive from key ID (production must use KMS)
        byte[] raw = keyId.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(raw, 0, key, 0, Math.min(raw.length, 32));
        return key;
    }
}