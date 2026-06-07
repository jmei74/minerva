package com.minerva.creditcard.controller;

import com.minerva.creditcard.repository.AccountRepository;
import com.minerva.creditcard.repository.AuthorizationRepository;
import com.minerva.creditcard.repository.CreditLimitAdjustmentRepository;
import com.minerva.creditcard.repository.TransactionRepository;
import com.minerva.creditcard.domain.entity.Transaction;
import com.minerva.creditcard.domain.entity.Authorization;
import com.minerva.creditcard.domain.entity.CreditLimitAdjustment;
import com.minerva.creditcard.domain.enums.AccountStatus;
import com.minerva.creditcard.domain.enums.TransactionStatus;
import com.minerva.creditcard.domain.enums.TransactionType;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Journey 监控 API
 * SkyEye 面板数据源
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final TransactionRepository transactionRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CreditLimitAdjustmentRepository adjustmentRepository;
    private final AccountRepository accountRepository;

    public MetricsController(TransactionRepository transactionRepository,
                            AuthorizationRepository authorizationRepository,
                            CreditLimitAdjustmentRepository adjustmentRepository,
                            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.authorizationRepository = authorizationRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * GET /api/v1/metrics/journeys
     * 返回各业务 journey 的状态统计
     */
    @GetMapping("/journeys")
    public Map<String, Object> getJourneys() {
        // ── 交易类 journey ───────────────────────────────
        Map<String, Long> txnByStatus = new HashMap<>();
        for (TransactionStatus s : TransactionStatus.values()) {
            txnByStatus.put(s.name(), 0L);
        }

        List<Transaction> allTxns = transactionRepository.findAll();
        for (Transaction txn : allTxns) {
            String key = txn.getStatus().name();
            txnByStatus.merge(key, 1L, Long::sum);
        }

        // ── 授权 journey ───────────────────────────────
        Map<String, Long> authByStatus = new LinkedHashMap<>();
        authByStatus.put("PENDING", 0L);
        authByStatus.put("COMPLETED", 0L);
        authByStatus.put("EXPIRED", 0L);
        authByStatus.put("CANCELLED", 0L);

        List<Authorization> allAuths = authorizationRepository.findAll();
        for (Authorization auth : allAuths) {
            String key = auth.getStatus().name();
            if (authByStatus.containsKey(key)) {
                authByStatus.merge(key, 1L, Long::sum);
            }
        }

        // ── 额度调整 journey ────────────────────────────
        Map<String, Long> adjByStatus = new LinkedHashMap<>();
        adjByStatus.put("PENDING", 0L);
        adjByStatus.put("APPROVED", 0L);
        adjByStatus.put("REJECTED", 0L);

        for (CreditLimitAdjustment adj : adjustmentRepository.findAll()) {
            String key = adj.getStatus() != null ? adj.getStatus().toUpperCase() : "APPROVED";
            if (adjByStatus.containsKey(key)) {
                adjByStatus.merge(key, 1L, Long::sum);
            }
        }

        // ── 账户状态 journey ───────────────────────────
        Map<String, Long> acctByStatus = new LinkedHashMap<>();
        for (AccountStatus s : AccountStatus.values()) {
            acctByStatus.put(s.name(), 0L);
        }
        accountRepository.findAll().forEach(acct -> {
            String key = acct.getStatus().name();
            acctByStatus.merge(key, 1L, Long::sum);
        });

        // ── 交易类型分布 ──────────────────────────────
        Map<String, Long> txnByType = new LinkedHashMap<>();
        for (TransactionType t : TransactionType.values()) {
            txnByType.put(t.name(), 0L);
        }
        for (Transaction txn : allTxns) {
            txnByType.merge(txn.getTxnType().name(), 1L, Long::sum);
        }

        long totalTxns = allTxns.size();
        long totalAuths = allAuths.size();
        long successTxns = allTxns.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();
        long successAuths = allAuths.stream().filter(a -> "COMPLETED".equals(a.getStatus().name())).count();
        long pendingTxns = allTxns.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();
        long pendingAuths = allAuths.stream().filter(a -> "PENDING".equals(a.getStatus().name())).count();

        Map<String, Object> journeys = new LinkedHashMap<>();

        journeys.put("授权 (PRE_AUTH)", Map.of(
                "icon","🔐",
                "description", "联机消费授权，额度冻结",
                "pending", pendingAuths,
                "completed", authByStatus.getOrDefault("COMPLETED", 0L),
                "expired", authByStatus.getOrDefault("EXPIRED", 0L),
                "cancelled", authByStatus.getOrDefault("CANCELLED", 0L),
                "total", totalAuths,
                "successRate", totalAuths > 0 ? (successAuths * 100.0 / totalAuths) : 0.0,
                "statusBreakdown", authByStatus
        ));

        journeys.put("消费清算 (PURCHASE)", Map.of(
                "icon", "💳",
                "description", "授权确认，完成清算扣款",
                "pending", txnByStatus.getOrDefault("PENDING", 0L),
                "completed", txnByStatus.getOrDefault("COMPLETED", 0L),
                "reversed", txnByStatus.getOrDefault("REVERSED", 0L),
                "failed", txnByStatus.getOrDefault("FAILED", 0L),
                "total", totalTxns,
                "successRate", totalTxns > 0 ? (successTxns * 100.0 / totalTxns) : 0.0,
                "statusBreakdown", txnByStatus
        ));

        journeys.put("退货 (REFUND)", Map.of(
                "icon", "↩️",
                "description", "消费退货，退款到账户",
                "pending", 0L,
                "completed", txnByType.getOrDefault("REFUND", 0L),
                "total", txnByType.getOrDefault("REFUND", 0L),
                "successRate", txnByType.getOrDefault("REFUND", 0L) > 0 ? 100.0 : 0.0,
                "statusBreakdown", Map.of("COMPLETED", txnByType.getOrDefault("REFUND", 0L))
        ));

        journeys.put("还款 (REPAYMENT)", Map.of(
                "icon", "💰",
                "description", "持卡人主动还款，余额清零",
                "pending", 0L,
                "completed", txnByType.getOrDefault("REPAYMENT", 0L),
                "total", txnByType.getOrDefault("REPAYMENT", 0L),
                "successRate", txnByType.getOrDefault("REPAYMENT", 0L) > 0 ? 100.0 : 0.0,
                "statusBreakdown", Map.of("COMPLETED", txnByType.getOrDefault("REPAYMENT", 0L))
        ));

        journeys.put("额度调整 (LIMIT)", Map.of(
                "icon", "📈",
                "description", "永久或临时额度调整申请",
                "pending", adjByStatus.getOrDefault("PENDING", 0L),
                "completed", adjByStatus.getOrDefault("APPROVED", 0L),
                "rejected", adjByStatus.getOrDefault("REJECTED", 0L),
                "total", adjustmentRepository.count(),
                "successRate", adjustmentRepository.count() > 0
                        ? (adjByStatus.getOrDefault("APPROVED", 0L) * 100.0 / adjustmentRepository.count()) : 0.0,
                "statusBreakdown", adjByStatus
        ));

        journeys.put("账户状态 (STATUS)", Map.of(
                "icon", "🏦",
                "description", "账户开/关/冻结状态变更",
                "active", acctByStatus.getOrDefault("ACTIVE", 0L),
                "pending", acctByStatus.getOrDefault("PENDING", 0L),
                "frozen", acctByStatus.getOrDefault("FROZEN", 0L),
                "closed", acctByStatus.getOrDefault("CLOSED", 0L),
                "total", accountRepository.count(),
                "statusBreakdown", acctByStatus
        ));

        return Map.of(
                "timestamp", new Date(),
                "journeys", journeys,
                "summary", Map.of(
                        "totalTransactions", totalTxns,
                        "totalAuths", totalAuths,
                        "pendingTransactions", pendingTxns,
                        "pendingAuths", pendingAuths,
                        "overallSuccessRate", (totalTxns + totalAuths) > 0
                                ? ((successTxns + successAuths) * 100.0 / (totalTxns + totalAuths)) : 0.0
                )
        );
    }
}