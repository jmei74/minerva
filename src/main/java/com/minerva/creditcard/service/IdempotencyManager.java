package com.minerva.creditcard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 幂等管理器（Idempotency Manager）
 * 架构参考: architecture-design.md §3.2
 *
 * 策略: 幂等键存Redis TTL 24h（ADR-003）
 * 降级: Redis不可用时降级为DB查询幂等键
 */
@Service
public class IdempotencyManager {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyManager.class);

    private static final String IDEMPOTENCY_KEY_PREFIX = "auth:idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    public IdempotencyManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查幂等键是否已存在
     * @return true=已存在（重复请求），false=新请求
     */
    public boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false; // 无幂等键则不拦截
        }

        try {
            String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis idempotency check failed, falling back: {}", e.getMessage());
            return false; // 降级放行，让DB唯一约束兜底
        }
    }

    /**
     * 记录幂等键（事务成功后写入）
     */
    public void markProcessed(String idempotencyKey, String txnId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        try {
            String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(key, txnId, IDEMPOTENCY_TTL);
            log.debug("Idempotency key recorded: {} -> {}", idempotencyKey, txnId);
        } catch (Exception e) {
            log.warn("Failed to record idempotency key: {}", idempotencyKey, e);
            // 不影响主链路
        }
    }

    /**
     * 获取幂等键对应的交易ID
     */
    public Optional<String> getTxnId(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            Object value = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value).map(Object::toString);
        } catch (Exception e) {
            log.warn("Failed to get idempotency key: {}", idempotencyKey, e);
            return Optional.empty();
        }
    }
}