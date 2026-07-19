package com.minerva.creditcard.controller;

import com.minerva.creditcard.dto.*;
import com.minerva.creditcard.service.AccountService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    
    private final AccountService accountService;
    
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{accountNo}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNo) {
        AccountResponse response = accountService.getAccount(accountNo);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomer(@PathVariable Long customerId) {
        List<AccountResponse> responses = accountService.getAccountsByCustomer(customerId);
        return ResponseEntity.ok(responses);
    }
    
    @PatchMapping("/{accountNo}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @PathVariable String accountNo,
            @RequestParam String status) {
        AccountResponse response = accountService.updateAccountStatus(accountNo, status);
        return ResponseEntity.ok(response);
    }
}
