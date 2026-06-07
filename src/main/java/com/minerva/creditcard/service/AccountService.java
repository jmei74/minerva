package com.minerva.creditcard.service;

import com.minerva.creditcard.domain.entity.Account;
import com.minerva.creditcard.domain.entity.CreditLimitAdjustment;
import com.minerva.creditcard.domain.enums.AccountStatus;
import com.minerva.creditcard.dto.AccountResponse;
import com.minerva.creditcard.dto.CreateAccountRequest;
import com.minerva.creditcard.dto.LimitAdjustmentRequest;
import com.minerva.creditcard.exception.AccountNotFoundException;
import com.minerva.creditcard.exception.BusinessRuleException;
import com.minerva.creditcard.repository.AccountRepository;
import com.minerva.creditcard.repository.CreditLimitAdjustmentRepository;
import com.minerva.creditcard.util.CardEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 账户服务（Account Service）
 * 架构参考: architecture-design.md §3.1
 *
 * 核心约束:
 * - §3.1 账户创建：先落库再发事件，失败回滚
 * - §3.1 状态变更：需校验无未清账款方可销户
 * - §3.1 敏感数据：AES-256加密存储card_no
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final CreditLimitAdjustmentRepository adjustmentRepository;
    private final CreditEngine creditEngine;
    private final CardEncryptionUtil cardEncryptionUtil;

    public AccountService(AccountRepository accountRepository,
                         CreditLimitAdjustmentRepository adjustmentRepository,
                         CreditEngine creditEngine,
                         CardEncryptionUtil cardEncryptionUtil) {
        this.accountRepository = accountRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.creditEngine = creditEngine;
        this.cardEncryptionUtil = cardEncryptionUtil;
    }

    /**
     * 创建账户（开卡）
     * 架构约束: 先落库再发事件，失败回滚
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for customer: {}", request.getCustomerId());

        // 1. 加密卡号
        String encrypted = cardEncryptionUtil.encrypt(request.getCardNo());
        String last4 = request.getCardNo().substring(request.getCardNo().length() - 4);

        // 2. 构建账户
        Account account = new Account();
        account.setCustomerId(request.getCustomerId());
        account.setCardNoEncrypted(encrypted);
        account.setCardNoLast4(last4);
        account.setCreditLimit(request.getCreditLimit());
        account.setTempLimit(request.getTempLimit() != null ? request.getTempLimit() : BigDecimal.ZERO);
        account.setUsedAmount(BigDecimal.ZERO);
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setBillingDay(request.getBillingDay());
        account.setDueDays(request.getDueDays() != null ? request.getDueDays() : 20);
        account.setOverLimitRatio(new BigDecimal("0.1000"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setOpenDate(LocalDate.now());

        // 3. 落库（失败自动回滚）
        Account saved = accountRepository.save(account);

        // 4. 初始化额度快照到Redis
        creditEngine.refreshSnapshot(saved);

        log.info("Account created: {}", saved.getAccountId());
        return toResponse(saved);
    }

    /**
     * 查询账户
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        return toResponse(account);
    }

    /**
     * 按卡号后4位查询账户
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountByCardLast4(String last4) {
        Account account = accountRepository.findByCardNoLast4(last4)
                .orElseThrow(() -> new AccountNotFoundException("Account not found by card: " + last4));
        return toResponse(account);
    }

    /**
     * 额度调整（永久/临时）
     * 架构约束: §3.1 额度管理（含审批流触发）
     */
    @Transactional
    public AccountResponse adjustLimit(UUID accountId, LimitAdjustmentRequest request) {
        log.info("Adjusting limit for account: {}, type: {}, newLimit: {}",
                accountId, request.getType(), request.getNewLimit());

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        // 记录调整历史
        CreditLimitAdjustment adjustment = new CreditLimitAdjustment();
        adjustment.setAccountId(accountId);
        adjustment.setAdjType(request.getType());
        adjustment.setOldLimit("PERMANENT".equals(request.getType())
                ? account.getCreditLimit() : account.getTempLimit());
        adjustment.setNewLimit(request.getNewLimit());
        adjustment.setReason(request.getReason());
        adjustment.setApprovalId(request.getApprovalId());
        adjustment.setOperatorId(request.getOperatorId());
        adjustment.setStatus("APPROVED");
        adjustmentRepository.save(adjustment);

        // 更新额度
        if ("PERMANENT".equals(request.getType())) {
            account.setCreditLimit(request.getNewLimit());
        } else {
            account.setTempLimit(request.getNewLimit());
        }

        Account saved = accountRepository.save(account);

        // 刷新快照
        creditEngine.refreshSnapshot(saved);

        log.info("Limit adjusted for account: {}, new available: {}",
                accountId, saved.getAvailableAmount());
        return toResponse(saved);
    }

    /**
     * 账户状态变更
     * 架构约束: §3.1 状态机 ACTIVE → FROZEN → CLOSED
     * §3.1 销户需校验无未清账款
     */
    @Transactional
    public AccountResponse changeStatus(UUID accountId, String newStatus, String reason) {
        log.info("Changing status for account: {} to {}, reason: {}",
                accountId, newStatus, reason);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        AccountStatus targetStatus = AccountStatus.valueOf(newStatus.toUpperCase());

        // 业务规则校验
        validateStatusTransition(account, targetStatus);

        account.setStatus(targetStatus);

        if (targetStatus == AccountStatus.CLOSED) {
            account.setCloseDate(LocalDate.now());
        }

        Account saved = accountRepository.save(account);

        // 失效快照
        creditEngine.invalidateSnapshot(accountId);

        log.info("Account status changed: {} -> {}", accountId, targetStatus);
        return toResponse(saved);
    }

    /**
     * 状态机转换规则校验
     */
    private void validateStatusTransition(Account account, AccountStatus target) {
        AccountStatus current = account.getStatus();

        switch (target) {
            case CLOSED -> {
                // 销户校验：无未清账款
                if (account.getUsedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    throw new BusinessRuleException(
                            "Cannot close account with outstanding balance: " + account.getUsedAmount());
                }
                if (account.getFrozenAmount().compareTo(BigDecimal.ZERO) > 0) {
                    throw new BusinessRuleException(
                            "Cannot close account with frozen amount: " + account.getFrozenAmount());
                }
            }
            case ACTIVE -> {
                // 只能从PENDING激活
                if (current != AccountStatus.PENDING) {
                    throw new BusinessRuleException(
                            "Can only activate PENDING account, current: " + current);
                }
            }
            case FROZEN -> {
                // 任意状态可冻结（风控场景）
            }
            default -> {
            }
        }
    }

    /**
     * 转换为响应DTO（掩码卡号）
     */
    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .customerId(account.getCustomerId())
                .cardNoMasked(CardEncryptionUtil.mask(account.getCardNoLast4()))
                .creditLimit(account.getCreditLimit())
                .tempLimit(account.getTempLimit())
                .usedAmount(account.getUsedAmount())
                .frozenAmount(account.getFrozenAmount())
                .availableAmount(account.getAvailableAmount())
                .billingDay(account.getBillingDay())
                .dueDays(account.getDueDays())
                .status(account.getStatus().name())
                .openDate(account.getOpenDate().toString())
                .closeDate(account.getCloseDate() != null ? account.getCloseDate().toString() : null)
                .createdAt(account.getCreatedAt().toString())
                .build();
    }
}