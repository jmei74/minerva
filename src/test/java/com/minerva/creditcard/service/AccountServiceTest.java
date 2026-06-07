package com.minerva.creditcard.service;

import com.minerva.creditcard.domain.entity.Account;
import com.minerva.creditcard.domain.enums.AccountStatus;
import com.minerva.creditcard.dto.AccountResponse;
import com.minerva.creditcard.dto.CreateAccountRequest;
import com.minerva.creditcard.dto.LimitAdjustmentRequest;
import com.minerva.creditcard.exception.AccountNotFoundException;
import com.minerva.creditcard.exception.BusinessRuleException;
import com.minerva.creditcard.repository.AccountRepository;
import com.minerva.creditcard.repository.CreditLimitAdjustmentRepository;
import com.minerva.creditcard.util.CardEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 账户服务单元测试
 * 架构参考: architecture-design.md §3.1
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CreditLimitAdjustmentRepository adjustmentRepository;

    @Mock
    private CreditEngine creditEngine;

    private AccountService accountService;

    private CardEncryptionUtil encryptionUtil;
    private Account testAccount;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        encryptionUtil = new CardEncryptionUtil(
                "minerva-credit-card-key-32bytes!".getBytes());
        accountService = new AccountService(
                accountRepository, adjustmentRepository, creditEngine, encryptionUtil);

        testAccountId = UUID.randomUUID();
        testAccount = new Account();
        testAccount.setAccountId(testAccountId);
        testAccount.setCustomerId(UUID.randomUUID());
        testAccount.setCardNoEncrypted(encryptionUtil.encrypt("6288888888881234"));
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
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setUpdatedAt(LocalDateTime.now());
        testAccount.setVersion(0L);
    }

    @Test
    @DisplayName("创建账户成功，卡号加密存储，后4位正确")
    void testCreateAccount() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setCardNo("6288888888881234");
        request.setCreditLimit(new BigDecimal("50000"));
        request.setTempLimit(new BigDecimal("10000"));
        request.setBillingDay(15);
        request.setDueDays(20);

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setAccountId(testAccountId);
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            return a;
        });

        AccountResponse response = accountService.createAccount(request);

        assertNotNull(response.accountId);
        assertEquals("**** **** **** 1234", response.cardNoMasked);
        assertEquals("ACTIVE", response.status);
        assertEquals(new BigDecimal("60000"),
                response.creditLimit.add(response.tempLimit));
        // available = creditLimit(50000) + tempLimit(10000) - usedAmount(0) - frozenAmount(0) = 60000
        assertEquals(new BigDecimal("60000"), response.availableAmount);

        verify(creditEngine).refreshSnapshot(any(Account.class));
    }

    @Test
    @DisplayName("查询账户成功")
    void testGetAccount() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        AccountResponse response = accountService.getAccount(testAccountId);

        assertEquals(testAccountId, response.accountId);
        assertEquals("**** **** **** 1234", response.cardNoMasked);
    }

    @Test
    @DisplayName("查询不存在的账户抛出异常")
    void testGetAccountNotFound() {
        when(accountRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                accountService.getAccount(UUID.randomUUID()));
    }

    @Test
    @DisplayName("永久额度调整成功")
    void testAdjustLimitPermanent() {
        when(accountRepository.findByIdForUpdate(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adjustmentRepository.save(any())).thenReturn(null);

        LimitAdjustmentRequest request = new LimitAdjustmentRequest();
        request.setNewLimit(new BigDecimal("60000"));
        request.setType("PERMANENT");
        request.setReason("提额审批");
        request.setOperatorId("ADMIN");

        AccountResponse response = accountService.adjustLimit(testAccountId, request);

        assertEquals(new BigDecimal("60000"), response.creditLimit);
        // 可用额度 = 60000 + 10000 - 15000 - 2000 = 53000
        assertEquals(new BigDecimal("53000"), response.availableAmount);
        verify(creditEngine).refreshSnapshot(any(Account.class));
    }

    @Test
    @DisplayName("临时额度调整成功")
    void testAdjustLimitTemp() {
        when(accountRepository.findByIdForUpdate(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adjustmentRepository.save(any())).thenReturn(null);

        LimitAdjustmentRequest request = new LimitAdjustmentRequest();
        request.setNewLimit(new BigDecimal("20000"));
        request.setType("TEMP");
        request.setReason("临时提额");

        AccountResponse response = accountService.adjustLimit(testAccountId, request);

        assertEquals(new BigDecimal("20000"), response.tempLimit);
        assertEquals(new BigDecimal("53000"), response.availableAmount);
    }

    @Test
    @DisplayName("冻结活跃账户成功")
    void testChangeStatusToFrozen() {
        when(accountRepository.findByIdForUpdate(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.changeStatus(testAccountId, "FROZEN", "欺诈嫌疑");

        assertEquals("FROZEN", response.status);
        verify(creditEngine).invalidateSnapshot(testAccountId);
    }

    @Test
    @DisplayName("有未清账款时销户被拒绝")
    void testCloseAccountWithOutstandingBalance() {
        testAccount.setUsedAmount(new BigDecimal("1000"));
        when(accountRepository.findByIdForUpdate(testAccountId)).thenReturn(Optional.of(testAccount));

        assertThrows(BusinessRuleException.class, () ->
                accountService.changeStatus(testAccountId, "CLOSED", "客户申请"));
    }

    @Test
    @DisplayName("有冻结金额时销户被拒绝")
    void testCloseAccountWithFrozenAmount() {
        testAccount.setUsedAmount(BigDecimal.ZERO);
        testAccount.setFrozenAmount(new BigDecimal("500"));
        when(accountRepository.findByIdForUpdate(testAccountId)).thenReturn(Optional.of(testAccount));

        assertThrows(BusinessRuleException.class, () ->
                accountService.changeStatus(testAccountId, "CLOSED", "客户申请"));
    }

    @Test
    @DisplayName("无欠款销户成功")
    void testCloseAccountSuccess() {
        testAccount.setUsedAmount(BigDecimal.ZERO);
        testAccount.setFrozenAmount(BigDecimal.ZERO);
        when(accountRepository.findByIdForUpdate(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.changeStatus(testAccountId, "CLOSED", "客户申请");

        assertEquals("CLOSED", response.status);
        assertNotNull(response.closeDate);
    }
}
