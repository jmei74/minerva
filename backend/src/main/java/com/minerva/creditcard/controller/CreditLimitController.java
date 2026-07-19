package com.minerva.creditcard.controller;

import com.minerva.creditcard.domain.CreditLimit;
import com.minerva.creditcard.service.CreditLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/credit-limits")
public class CreditLimitController {
    
    private final CreditLimitService creditLimitService;
    
    public CreditLimitController(CreditLimitService creditLimitService) {
        this.creditLimitService = creditLimitService;
    }
    
    @GetMapping("/account/{accountId}/available")
    public ResponseEntity<BigDecimal> getAvailableCredit(@PathVariable Long accountId) {
        BigDecimal available = creditLimitService.getAvailableCredit(accountId);
        return ResponseEntity.ok(available);
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<CreditLimit> getCreditLimit(@PathVariable Long accountId) {
        CreditLimit creditLimit = creditLimitService.getCreditLimit(accountId);
        return ResponseEntity.ok(creditLimit);
    }
    
    @PatchMapping("/account/{accountId}/adjust")
    public ResponseEntity<CreditLimit> adjustCreditLimit(
            @PathVariable Long accountId,
            @RequestParam BigDecimal newLimit) {
        CreditLimit creditLimit = creditLimitService.adjustCreditLimit(accountId, newLimit);
        return ResponseEntity.ok(creditLimit);
    }
    
    @PatchMapping("/account/{accountId}/temporary")
    public ResponseEntity<CreditLimit> setTemporaryLimit(
            @PathVariable Long accountId,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate expiryDate) {
        CreditLimit creditLimit = creditLimitService.setTemporaryLimit(accountId, amount, expiryDate);
        return ResponseEntity.ok(creditLimit);
    }
}
