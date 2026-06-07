package com.minerva.creditcard.service;

import com.minerva.creditcard.domain.entity.Account;
import com.minerva.creditcard.domain.entity.Authorization;
import com.minerva.creditcard.domain.entity.Transaction;
import com.minerva.creditcard.domain.enums.AuthorizationStatus;
import com.minerva.creditcard.domain.enums.AuthorizationType;
import com.minerva.creditcard.domain.enums.TransactionStatus;
import com.minerva.creditcard.domain.enums.TransactionType;
import com.minerva.creditcard.dto.AuthorizationRequest;
import com.minerva.creditcard.dto.AuthorizationResponse;
import com.minerva.creditcard.exception.AccountNotFoundException;
import com.minerva.creditcard.exception.InsufficientCreditException;
import com.minerva.creditcard.repository.AccountRepository;
import com.minerva.creditcard.repository.AuthorizationRepository;
import com.minerva.creditcard.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 交易服务（Transaction Service）
 * 架构参考: architecture-design.md §3.2
 *
 * 核心约束:
 * - §3.2 授权链路：先查Redis额度快照判断是否通过，通过后乐观锁更新DB，异步写流水
 * - §3.2 幂等：基于auth_code/reference_no做幂等校验，存储在Redis（TTL 24h）
 * - §3.2 并发控制：乐观锁（version字段）+ 重试机制（最多3次）
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CreditEngine creditEngine;
    private final IdempotencyManager idempotencyManager;
    private final KafkaEventService kafkaEventService;

    private static final int MAX_RETRY = 3;

    public TransactionService(AccountRepository accountRepository,
                             TransactionRepository transactionRepository,
                             AuthorizationRepository authorizationRepository,
                             CreditEngine creditEngine,
                             IdempotencyManager idempotencyManager,
                             KafkaEventService kafkaEventService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.authorizationRepository = authorizationRepository;
        this.creditEngine = creditEngine;
        this.idempotencyManager = idempotencyManager;
        this.kafkaEventService = kafkaEventService;
    }

    /**
     * 消费授权（联机授权，核心链路 P99 < 100ms）
     * 架构约束: §3.2 + §12 授权 P99 < 100ms
     */
    @Transactional
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        log.info("Authorization request: card={}, amount={}, merchant={}",
                request.getCardNo(), request.getAmount(), request.getMerchantId());

        // 1. 幂等检查（Redis）
        if (idempotencyManager.isDuplicate(request.getIdempotencyKey())) {
            log.warn("Duplicate authorization request: {}", request.getIdempotencyKey());
            String existingTxnId = idempotencyManager.getTxnId(request.getIdempotencyKey()).orElse("");
            AuthorizationResponse response = new AuthorizationResponse();
            response.status = "DUPLICATE";
            response.referenceNo = existingTxnId;
            return response;
        }

        // 2. 按卡号后4位查询账户
        String last4 = request.getCardNo().substring(request.getCardNo().length() - 4);
        Account account = accountRepository.findByCardNoLast4(last4)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + last4));

        // 3. 账户状态检查
        if (!account.isActive()) {
            return AuthorizationResponse.declined("ACCOUNT_NOT_ACTIVE",
                    "Account is not active: " + account.getStatus());
        }

        // 4. 额度预检查（Redis快照，5s TTL）
        if (!creditEngine.canAuthorize(account.getAccountId(), request.getAmount())) {
            return AuthorizationResponse.declined("INSUFFICIENT_CREDIT", "Insufficient credit");
        }

        // 5. 生成授权码和参考号
        String authCode = generateAuthCode();
        String referenceNo = generateReferenceNo();

        // 6. 冻结额度（乐观锁更新DB）
        BigDecimal availableAfter;
        try {
            availableAfter = creditEngine.freeze(account.getAccountId(), request.getAmount());
        } catch (Exception e) {
            log.warn("Credit freeze failed, declining: {}", e.getMessage());
            return AuthorizationResponse.declined("SYSTEM_ERROR", "Unable to process authorization");
        }

        // 7. 创建授权记录
        Authorization authorization = new Authorization();
        authorization.setAccountId(account.getAccountId());
        authorization.setAuthCode(authCode);
        authorization.setAuthType(determineAuthType(request.getTxnType()));
        authorization.setAuthAmount(request.getAmount());
        authorization.setConsumedAmount(BigDecimal.ZERO);
        authorization.setMerchantId(request.getMerchantId());
        authorization.setMerchantName(request.getMerchantName());
        authorization.setTerminalId(request.getTerminalId());
        authorization.setStatus(AuthorizationStatus.PENDING);
        authorization.setExpireTime(LocalDateTime.now().plusDays(30));
        authorizationRepository.save(authorization);

        // 8. 记录幂等键
        if (request.getIdempotencyKey() != null) {
            idempotencyManager.markProcessed(request.getIdempotencyKey(), referenceNo);
        }

        log.info("Authorization approved: authCode={}, account={}, available={}",
                authCode, account.getAccountId(), availableAfter);

        // Kafka 事件
        kafkaEventService.publishAuthorization(
                authCode,
                account.getAccountId().toString(),
                request.getCardNo(),
                request.getTxnType(),
                request.getAmount().doubleValue(),
                request.getMerchantId(),
                request.getMerchantName(),
                authorization.getStatus().name()
        );

        return AuthorizationResponse.approved(authCode, referenceNo, availableAfter);
    }

    /**
     * 授权完成（清算）
     * 架构约束: §3.2 消费确认（清算）
     */
    @Transactional
    public Transaction settle(String authCode, BigDecimal settleAmount) {
        log.info("Settlement request: authCode={}, amount={}", authCode, settleAmount);

        Authorization authorization = authorizationRepository.findByAuthCode(authCode)
                .orElseThrow(() -> new IllegalArgumentException("Authorization not found: " + authCode));

        if (authorization.getStatus() != AuthorizationStatus.PENDING) {
            throw new IllegalStateException("Authorization is not pending: " + authorization.getStatus());
        }

        // 1. 冻结转已用
        BigDecimal availableAfter = creditEngine.increaseUsed(
                authorization.getAccountId(), settleAmount);

        // 2. 更新授权记录
        authorization.setConsumedAmount(authorization.getConsumedAmount().add(settleAmount));
        if (authorization.isFullyConsumed()) {
            authorization.setStatus(AuthorizationStatus.COMPLETED);
        }
        authorizationRepository.save(authorization);

        // 3. 记录交易流水
        Transaction txn = new Transaction();
        txn.setAccountId(authorization.getAccountId());
        txn.setTxnType(TransactionType.AUTH_COMPLETION);
        txn.setTxnAmount(settleAmount);
        txn.setCurrency("CNY");
        txn.setMerchantId(authorization.getMerchantId());
        txn.setMerchantName(authorization.getMerchantName());
        txn.setTerminalId(authorization.getTerminalId());
        txn.setAuthCode(authCode);
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setTxnTime(LocalDateTime.now());
        txn.setAvailableAmountBefore(availableAfter.add(settleAmount));
        txn.setAvailableAmountAfter(availableAfter);
        Transaction saved = transactionRepository.save(txn);

        // Kafka 事件
        kafkaEventService.publishTransaction(
                saved.getTxnId().toString(),
                authorization.getAccountId().toString(),
                saved.getTxnType().name(),
                saved.getTxnAmount().doubleValue(),
                saved.getAvailableAmountBefore().doubleValue(),
                saved.getAvailableAmountAfter().doubleValue(),
                saved.getStatus().name()
        );

        return saved;
    }

    /**
     * 退货
     * 架构约束: §3.2 退货
     */
    @Transactional
    public Transaction refund(UUID accountId, BigDecimal refundAmount, UUID originalTxnId) {
        log.info("Refund request: account={}, amount={}, originalTxn={}",
                accountId, refundAmount, originalTxnId);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        // 1. 增加可用额度（退货释放已用金额）
        BigDecimal availableAfter = creditEngine.decreaseUsed(accountId, refundAmount);

        // 2. 记录交易流水
        Transaction txn = new Transaction();
        txn.setAccountId(accountId);
        txn.setTxnType(TransactionType.REFUND);
        txn.setTxnAmount(refundAmount);
        txn.setCurrency("CNY");
        txn.setAvailableAmountBefore(availableAfter.add(refundAmount));
        txn.setAvailableAmountAfter(availableAfter);
        txn.setReferenceNo("REFUND-" + originalTxnId);
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setTxnTime(LocalDateTime.now());
        Transaction saved = transactionRepository.save(txn);

        // Kafka 事件
        kafkaEventService.publishTransaction(
                saved.getTxnId().toString(),
                accountId.toString(),
                saved.getTxnType().name(),
                saved.getTxnAmount().doubleValue(),
                saved.getAvailableAmountBefore().doubleValue(),
                saved.getAvailableAmountAfter().doubleValue(),
                saved.getStatus().name()
        );

        return saved;
    }

    /**
     * 还款
     * 架构约束: §3.2 还款
     */
    @Transactional
    public Transaction repay(UUID accountId, BigDecimal repayAmount) {
        log.info("Repayment request: account={}, amount={}", accountId, repayAmount);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        BigDecimal beforeAmount = account.getUsedAmount();
        BigDecimal newUsed = beforeAmount.subtract(repayAmount).max(BigDecimal.ZERO);

        account.setUsedAmount(newUsed);
        accountRepository.save(account);

        Transaction txn = new Transaction();
        txn.setAccountId(accountId);
        txn.setTxnType(TransactionType.REPAYMENT);
        txn.setTxnAmount(repayAmount);
        txn.setCurrency("CNY");
        txn.setAvailableAmountBefore(account.getAvailableAmount().add(repayAmount));
        txn.setAvailableAmountAfter(account.getAvailableAmount());
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setTxnTime(LocalDateTime.now());
        Transaction saved = transactionRepository.save(txn);

        // Kafka 事件
        kafkaEventService.publishRepayment(
                saved.getTxnId().toString(),
                accountId.toString(),
                saved.getTxnAmount().doubleValue(),
                saved.getAvailableAmountBefore().doubleValue(),
                saved.getAvailableAmountAfter().doubleValue(),
                saved.getStatus().name()
        );

        return saved;
    }

    private AuthorizationType determineAuthType(String txnType) {
        if ("PRE_AUTH".equals(txnType)) {
            return AuthorizationType.PRE_AUTH;
        }
        return AuthorizationType.PRE_AUTH; // 默认预授权
    }

    private String generateAuthCode() {
        return "A" + String.format("%05d", (int) (Math.random() * 99999));
    }

    private String generateReferenceNo() {
        return "RN" + System.currentTimeMillis();
    }
}