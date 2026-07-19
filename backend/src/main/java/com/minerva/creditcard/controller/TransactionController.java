package com.minerva.creditcard.controller;

import com.minerva.creditcard.dto.*;
import com.minerva.creditcard.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizationRequest request) {
        AuthorizationResponse response = transactionService.authorize(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{transactionId}/capture")
    public ResponseEntity<TransactionResponse> capture(@PathVariable String transactionId) {
        TransactionResponse response = transactionService.capture(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refund")
    public ResponseEntity<TransactionResponse> refund(@Valid @RequestBody RefundRequest request) {
        TransactionResponse response = transactionService.refund(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/payment")
    public ResponseEntity<TransactionResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        TransactionResponse response = transactionService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        TransactionResponse response = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccount(@PathVariable Long accountId) {
        List<TransactionResponse> responses = transactionService.getTransactions(accountId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/account/{accountId}/history")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByDateRange(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<TransactionResponse> responses = transactionService.getTransactionsByDateRange(accountId, startDate, endDate);
        return ResponseEntity.ok(responses);
    }
}
