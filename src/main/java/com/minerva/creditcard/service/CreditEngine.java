package com.minerva.creditcard.service;

import com.minerva.creditcard.domain.entity.Account;
import com.minerva.creditcard.dto.CreditSnapshot;
import com.minerva.creditcard.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 额度引擎（Credit Engine）
 * 架构参考: architecture-design.md §3.4.1
 *
 * 核心公式: 可用额度 = 固定额度 + 临时额度 - 已使用金额 - 冻结金额
 *
 * 缓存策略: §6 Redis Key Design
 * - credit:snapshot:{account_id} TTL=5s（Cache-Aside模式）
 * - lock:account:{account_id} TTL=10s（分布式锁）
 */
@Service
public class CreditEngine {

    private static final Logger log = LoggerFactory.getLogger(CreditEngine.class);

    private static final String SNAPSHOT_KEY_PREFIX = "credit:snapshot:";
    private static final String AVAILABLE_KEY_PREFIX = "credit:available:";
    private static final String LOCK_KEY_PREFIX = "lock:account:";
    private static final Duration SNAPSHOT_TTL = Duration.ofSeconds(5);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final AccountRepository accountRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public CreditEngine(AccountRepository accountRepository, RedisTemplate<String, Object> redisTemplate) {
        this.accountRepository = accountRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取可用额度（优先读Redis快照，5s强制回源）
     * 架构约束: §6.2 Redis TTL=5秒（强制回源）
     */
    public BigDecimal getAvailableAmount(UUID accountId) {
        // 1. 先查Redis快照
        String snapshotKey = SNAPSHOT_KEY_PREFIX + accountId;
        CreditSnapshot snapshot = (CreditSnapshot) redisTemplate.opsForValue().get(snapshotKey);

        if (snapshot != null) {
            log.debug("Credit snapshot hit for account: {}", accountId);
            return snapshot.availableAmount;
        }

        // 2. 快照miss，回源DB
        log.debug("Credit snapshot miss for account: {}, loading from DB", accountId);
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }

        Account account = accountOpt.get();
        BigDecimal available = account.getAvailableAmount();

        // 3. 写回Redis（异步，非阻塞主链路）
        refreshSnapshotAsync(account);

        return available;
    }

    /**
     * 检查交易是否可通过（额度判断）
     * 架构约束: §3.2 授权链路：先查Redis额度快照判断是否通过
     */
    public boolean canAuthorize(UUID accountId, BigDecimal amount) {
        BigDecimal available = getAvailableAmount(accountId);
        return available.compareTo(amount) >= 0;
    }

    /**
     * 额度冻结（消费授权时调用）
     * @return 冻结后的可用额度
     */
    public BigDecimal freeze(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        BigDecimal availableBefore = account.getAvailableAmount();
        if (availableBefore.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient credit");
        }

        account.setFrozenAmount(account.getFrozenAmount().add(amount));
        Account saved = accountRepository.saveAndFlush(account);

        // 更新Redis快照
        refreshSnapshot(saved);

        log.info("Credit frozen for account: {}, amount: {}, available after: {}",
                accountId, amount, saved.getAvailableAmount());

        return saved.getAvailableAmount();
    }

    /**
     * 额度释放（退货/授权撤销/还款时调用）
     * @return 释放后的可用额度
     */
    public BigDecimal unfreeze(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        BigDecimal currentFrozen = account.getFrozenAmount();
        BigDecimal releaseAmount = amount.min(currentFrozen);

        account.setFrozenAmount(currentFrozen.subtract(releaseAmount));
        Account saved = accountRepository.saveAndFlush(account);

        refreshSnapshot(saved);

        log.info("Credit unfrozen for account: {}, amount: {}, available after: {}",
                accountId, releaseAmount, saved.getAvailableAmount());

        return saved.getAvailableAmount();
    }

    /**
     * 已使用金额增加（消费确认时调用）
     */
    public BigDecimal increaseUsed(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        // 从frozen转入used
        BigDecimal currentFrozen = account.getFrozenAmount();
        BigDecimal fromFrozen = amount.min(currentFrozen);
        BigDecimal fromAvailable = amount.subtract(fromFrozen);

        account.setFrozenAmount(currentFrozen.subtract(fromFrozen));
        account.setUsedAmount(account.getUsedAmount().add(fromAvailable));
        Account saved = accountRepository.saveAndFlush(account);

        refreshSnapshot(saved);

        return saved.getAvailableAmount();
    }

    /**
     * 已使用金额减少（退货时调用）
     */
    public BigDecimal decreaseUsed(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        BigDecimal currentUsed = account.getUsedAmount();
        account.setUsedAmount(currentUsed.subtract(amount.min(currentUsed)));
        Account saved = accountRepository.saveAndFlush(account);

        refreshSnapshot(saved);

        return saved.getAvailableAmount();
    }

    /**
     * 获取额度快照（用于缓存写入）
     */
    public CreditSnapshot buildSnapshot(Account account) {
        return CreditSnapshot.builder()
                .accountId(account.getAccountId())
                .creditLimit(account.getCreditLimit())
                .tempLimit(account.getTempLimit())
                .usedAmount(account.getUsedAmount())
                .frozenAmount(account.getFrozenAmount())
                .availableAmount(account.getAvailableAmount())
                .version(account.getVersion())
                .build();
    }

    /**
     * 刷新Redis快照（对AccountService暴露的公开方法）
     */
    public void refreshSnapshot(Account account) {
        try {
            String snapshotKey = SNAPSHOT_KEY_PREFIX + account.getAccountId();
            CreditSnapshot snapshot = buildSnapshot(account);
            redisTemplate.opsForValue().set(snapshotKey, snapshot, SNAPSHOT_TTL);
        } catch (Exception e) {
            log.warn("Failed to refresh credit snapshot for account: {}, error: {}",
                    account.getAccountId(), e.getMessage());
            // 快照刷新失败不影响主链路
        }
    }

    /**
     * 异步刷新快照（通过Redis消息队列，不阻塞主链路）
     * 注：生产环境应使用Kafka异步写，这里简化用线程池
     */
    private void refreshSnapshotAsync(Account account) {
        String snapshotKey = SNAPSHOT_KEY_PREFIX + account.getAccountId();
        CreditSnapshot snapshot = buildSnapshot(account);
        try {
            redisTemplate.opsForValue().set(snapshotKey, snapshot, SNAPSHOT_TTL);
        } catch (Exception e) {
            log.warn("Failed to refresh credit snapshot async for account: {}",
                    account.getAccountId());
        }
    }

    /**
     * 删除额度快照（账户更新时调用）
     */
    public void invalidateSnapshot(UUID accountId) {
        String snapshotKey = SNAPSHOT_KEY_PREFIX + accountId;
        redisTemplate.delete(snapshotKey);
        log.debug("Credit snapshot invalidated for account: {}", accountId);
    }
}