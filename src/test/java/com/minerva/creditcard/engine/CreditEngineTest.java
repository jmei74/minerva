package com.minerva.creditcard.engine;

import com.minerva.creditcard.domain.entity.Account;
import com.minerva.creditcard.domain.enums.AccountStatus;
import com.minerva.creditcard.dto.CreditSnapshot;
import com.minerva.creditcard.repository.AccountRepository;
import com.minerva.creditcard.service.CreditEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 额度引擎单元测试
 * 架构参考: architecture-design.md §3.4.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditEngineTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CreditEngine creditEngine;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        creditEngine = new CreditEngine(accountRepository, redisTemplate);

        testAccount = new Account();
        testAccount.setAccountId(UUID.randomUUID());
        testAccount.setCustomerId(UUID.randomUUID());
        testAccount.setCardNoEncrypted("encrypted");
        testAccount.setCardNoLast4("1234");
        testAccount.setCreditLimit(new BigDecimal("50000"));
        testAccount.setTempLimit(new BigDecimal("10000"));
        testAccount.setUsedAmount(new BigDecimal("15000"));
        testAccount.setFrozenAmount(new BigDecimal("2000"));
        testAccount.setBillingDay(15);
        testAccount.setDueDays(20);
        testAccount.setOverLimitRatio(new BigDecimal("0.1000"));
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setOpenDate(LocalDate.now());
        testAccount.setVersion(0L);
    }

    @Test
    @DisplayName("可用额度 = 固定额度 + 临时额度 - 已使用 - 冻结")
    void testAvailableAmountFormula() {
        // 50000 + 10000 - 15000 - 2000 = 43000
        BigDecimal expected = new BigDecimal("43000");
        assertEquals(expected, testAccount.getAvailableAmount());
    }

    @Test
    @DisplayName("Redis快照命中时直接返回可用额度")
    void testSnapshotHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CreditSnapshot snapshot = new CreditSnapshot();
        snapshot.accountId = testAccount.getAccountId();
        snapshot.creditLimit = new BigDecimal("50000");
        snapshot.tempLimit = new BigDecimal("10000");
        snapshot.usedAmount = new BigDecimal("15000");
        snapshot.frozenAmount = new BigDecimal("2000");
        snapshot.availableAmount = new BigDecimal("43000");
        snapshot.version = 1L;

        when(valueOperations.get("credit:snapshot:" + testAccount.getAccountId()))
                .thenReturn(snapshot);

        BigDecimal result = creditEngine.getAvailableAmount(testAccount.getAccountId());

        assertEquals(new BigDecimal("43000"), result);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Redis快照miss时回源DB并写回快照")
    void testSnapshotMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(accountRepository.findById(testAccount.getAccountId()))
                .thenReturn(Optional.of(testAccount));

        BigDecimal result = creditEngine.getAvailableAmount(testAccount.getAccountId());

        assertEquals(new BigDecimal("43000"), result);
        verify(valueOperations, atLeastOnce()).set(anyString(), any(CreditSnapshot.class), any());
    }

    @Test
    @DisplayName("额度冻结后可用额度正确减少")
    void testFreeze() {
        when(accountRepository.findByIdForUpdate(testAccount.getAccountId()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BigDecimal availableAfter = creditEngine.freeze(
                testAccount.getAccountId(), new BigDecimal("5000"));

        assertEquals(new BigDecimal("38000"), availableAfter); // 43000 - 5000
    }

    @Test
    @DisplayName("额度冻结失败抛出异常")
    void testFreezeInsufficient() {
        // available = 50000+10000-15000-50001 = -1 < 0, so amount 1000 will fail
        testAccount.setFrozenAmount(new BigDecimal("50001"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(accountRepository.findById(testAccount.getAccountId()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.findByIdForUpdate(testAccount.getAccountId()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThrows(IllegalArgumentException.class, () ->
                creditEngine.freeze(testAccount.getAccountId(), new BigDecimal("1000")));
    }

    @Test
    @DisplayName("额度释放后可用额度正确增加")
    void testUnfreeze() {
        // Initially frozen=2000, releasing 2000 fully frees it
        testAccount.setFrozenAmount(new BigDecimal("2000"));
        when(accountRepository.findByIdForUpdate(testAccount.getAccountId()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(inv -> {
                    Account a = inv.getArgument(0);
                    return a;
                });

        BigDecimal availableAfter = creditEngine.unfreeze(
                testAccount.getAccountId(), new BigDecimal("2000"));

        // After unfreeze(2000): frozen=2000-2000=0
        // available = 50000+10000-15000-0 = 45000 (increases from 43000)
        assertEquals(new BigDecimal("45000"), availableAfter);
    }

    @Test
    @DisplayName("消费确认后已用金额增加")
    void testIncreaseUsed() {
        testAccount.setFrozenAmount(new BigDecimal("5000"));
        when(accountRepository.findByIdForUpdate(testAccount.getAccountId()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BigDecimal availableAfter = creditEngine.increaseUsed(
                testAccount.getAccountId(), new BigDecimal("3000"));

        assertEquals(new BigDecimal("43000"), availableAfter);
    }

    @Test
    @DisplayName("退货后已用金额减少")
    void testDecreaseUsed() {
        testAccount.setUsedAmount(new BigDecimal("15000"));
        when(accountRepository.findByIdForUpdate(testAccount.getAccountId()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BigDecimal availableAfter = creditEngine.decreaseUsed(
                testAccount.getAccountId(), new BigDecimal("5000"));

        assertEquals(new BigDecimal("48000"), availableAfter); // 50000+10000-10000-2000
    }

    @Test
    @DisplayName("账户不存在时抛出异常")
    void testAccountNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(accountRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                creditEngine.getAvailableAmount(UUID.randomUUID()));
    }

    @Test
    @DisplayName("最大允许消费金额含超额比例")
    void testMaxAllowedAmount() {
        // available=43000, overLimitRatio=10%, max=43000*1.1=47300
        assertEquals(0, new BigDecimal("47300.000").compareTo(testAccount.getMaxAllowedAmount()));
    }
}
