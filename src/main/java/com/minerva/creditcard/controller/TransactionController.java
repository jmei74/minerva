package com.minerva.creditcard.controller;

import com.minerva.creditcard.dto.AuthorizationRequest;
import com.minerva.creditcard.dto.AuthorizationResponse;
import com.minerva.creditcard.domain.entity.Transaction;
import com.minerva.creditcard.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 交易处理 REST API
 * 架构参考: architecture-design.md §3.2
 */
@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ---- Request DTOs (defined first to avoid forward-reference issues) ----

    public static class SettleRequest {
        public String authCode;
        public BigDecimal amount;

        public SettleRequest() {
        }

        public SettleRequest(String authCode, BigDecimal amount) {
            this.authCode = authCode;
            this.amount = amount;
        }
    }

    public static class RefundRequest {
        public UUID accountId;
        public BigDecimal amount;

        public RefundRequest() {
        }

        public RefundRequest(UUID accountId, BigDecimal amount) {
            this.accountId = accountId;
            this.amount = amount;
        }
    }

    public static class RepayRequest {
        public BigDecimal amount;

        public RepayRequest() {
        }

        public RepayRequest(BigDecimal amount) {
            this.amount = amount;
        }
    }

    // ---- REST Endpoints ----

    /**
     * 消费授权（联机授权）
     * POST /api/v1/transactions/authorize
     * 架构约束: P99 < 100ms
     */
    @PostMapping("/transactions/authorize")
    public ResponseEntity<AuthorizationResponse> authorize(
            @Valid @RequestBody AuthorizationRequest request) {
        log.info("REST: authorize card={}, amount={}",
                request.getCardNo(), request.getAmount());
        AuthorizationResponse response = transactionService.authorize(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 消费确认（清算）
     * POST /api/v1/transactions/settle
     */
    @PostMapping("/transactions/settle")
    public ResponseEntity<Map<String, Object>> settle(@RequestBody SettleRequest request) {
        log.info("REST: settle authCode={}, amount={}", request.authCode, request.amount);
        Transaction txn = transactionService.settle(request.authCode, request.amount);
        return ResponseEntity.ok(Map.of(
                "txnId", txn.getTxnId(),
                "status", txn.getStatus().name()
        ));
    }

    /**
     * 退货
     * POST /api/v1/transactions/{txn_id}/refund
     */
    @PostMapping("/transactions/{txn_id}/refund")
    public ResponseEntity<Map<String, Object>> refund(
            @PathVariable("txn_id") UUID txnId,
            @RequestBody RefundRequest request) {
        log.info("REST: refund txnId={}, amount={}", txnId, request.amount);
        Transaction refundTxn = transactionService.refund(
                request.accountId, request.amount, txnId);
        return ResponseEntity.ok(Map.of(
                "refundTxnId", refundTxn.getTxnId(),
                "status", "COMPLETED"
        ));
    }

    /**
     * 还款
     * POST /api/v1/accounts/{account_id}/repayment
     */
    @PostMapping("/accounts/{account_id}/repayment")
    public ResponseEntity<Map<String, Object>> repay(
            @PathVariable("account_id") UUID accountId,
            @RequestBody RepayRequest request) {
        log.info("REST: repay accountId={}, amount={}", accountId, request.amount);
        Transaction txn = transactionService.repay(accountId, request.amount);
        return ResponseEntity.ok(Map.of(
                "txnId", txn.getTxnId(),
                "remainingBalance", txn.getAvailableAmountAfter()
        ));
    }
}