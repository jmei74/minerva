package com.minerva.creditcard.config;

import com.minerva.creditcard.util.CardEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 安全配置
 * 架构约束: §11 PCI-DSS - AES-256卡号加密
 *
 * 注: 生产环境应从 AWS KMS 获取密钥
 */
@Configuration
public class SecurityConfig {

    @Value("${aws.kms.key-id:}")
    private String kmsKeyId;

    @Bean
    public CardEncryptionUtil cardEncryptionUtil() {
        // 生产环境: 从 AWS KMS 获取实际密钥
        // 这里用配置的key-id派生（简化实现）
        byte[] keyBytes = deriveKey(kmsKeyId);
        return new CardEncryptionUtil(keyBytes);
    }

    /**
     * 从 KMS key-id 派生 32 字节 AES 密钥
     * 生产环境应调用 AWS KMS GenerateDataKey API
     */
    private byte[] deriveKey(String keyId) {
        // 简单演示：使用 key-id 的 UTF-8 字节填充/截断至 32 字节
        // WARNING: 生产环境必须使用真正的随机 AES-256 密钥
        if (keyId == null || keyId.isBlank()) {
            // 默认测试密钥（32字节）
            return "minerva-credit-card-key-32bytes!!".getBytes(StandardCharsets.UTF_8);
        }
        byte[] raw = keyId.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(raw, 0, key, 0, Math.min(raw.length, 32));
        return key;
    }
}