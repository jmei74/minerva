package com.minerva.creditcard.controller;

import com.minerva.creditcard.dto.AccountResponse;
import com.minerva.creditcard.dto.CreateAccountRequest;
import com.minerva.creditcard.dto.LimitAdjustmentRequest;
import com.minerva.creditcard.service.AccountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 账户管理 REST API
 * 架构参考: architecture-design.md §3.1
 *
 * API 设计:
 * - POST   /api/v1/accounts              — 创建账户（开卡）
 * - GET    /api/v1/accounts/{account_id} — 查询账户
 * - PUT    /api/v1/accounts/{account_id}/limit   — 额度调整
 * - PUT    /api/v1/accounts/{account_id}/status   — 账户状态变更
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 创建账户（开卡）
     * POST /api/v1/accounts
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        log.info("REST: createAccount customer={}", request.getCustomerId());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 查询账户
     * GET /api/v1/accounts/{account_id}
     */
    @GetMapping
    public ResponseEntity<?> listAccounts() {
        log.info("REST: listAccounts");
        return ResponseEntity.ok(accountService.listAllAccounts());
    }

    @GetMapping("/{account_id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable("account_id") UUID accountId) {
        log.info("REST: getAccount accountId={}", accountId);
        AccountResponse response = accountService.getAccount(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * 按卡号后4位查询账户
     * GET /api/v1/accounts/by-card/{last4}
     */
    @GetMapping("/by-card/{last4}")
    public ResponseEntity<AccountResponse> getAccountByCard(
            @PathVariable("last4") String last4) {
        log.info("REST: getAccountByCard last4={}", last4);
        AccountResponse response = accountService.getAccountByCardLast4(last4);
        return ResponseEntity.ok(response);
    }

    /**
     * 额度调整
     * PUT /api/v1/accounts/{account_id}/limit
     */
    @PutMapping("/{account_id}/limit")
    public ResponseEntity<AccountResponse> adjustLimit(
            @PathVariable("account_id") UUID accountId,
            @Valid @RequestBody LimitAdjustmentRequest request) {
        log.info("REST: adjustLimit accountId={}, type={}, newLimit={}",
                accountId, request.getType(), request.getNewLimit());
        AccountResponse response = accountService.adjustLimit(accountId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 账户状态变更
     * PUT /api/v1/accounts/{account_id}/status
     */
    @PutMapping("/{account_id}/status")
    public ResponseEntity<AccountResponse> changeStatus(
            @PathVariable("account_id") UUID accountId,
            @RequestBody StatusChangeRequest request) {
        log.info("REST: changeStatus accountId={}, status={}", accountId, request.status);
        AccountResponse response = accountService.changeStatus(
                accountId, request.status, request.reason);
        return ResponseEntity.ok(response);
    }

    public static class StatusChangeRequest {
        public String status;
        public String reason;
    }
}