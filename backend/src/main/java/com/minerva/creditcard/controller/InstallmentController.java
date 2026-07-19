package com.minerva.creditcard.controller;

import com.minerva.creditcard.dto.*;
import com.minerva.creditcard.service.InstallmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/installments")
@RequiredArgsConstructor
public class InstallmentController {
    
    private final InstallmentService installmentService;
    
    @PostMapping
    public ResponseEntity<InstallmentResponse> createInstallment(@Valid @RequestBody InstallmentRequest request) {
        InstallmentResponse response = installmentService.createInstallment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{scheduleId}")
    public ResponseEntity<InstallmentResponse> getInstallment(@PathVariable String scheduleId) {
        InstallmentResponse response = installmentService.getInstallment(scheduleId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<InstallmentResponse>> getInstallmentsByAccount(@PathVariable Long accountId) {
        List<InstallmentResponse> responses = installmentService.getInstallmentsByAccount(accountId);
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/{scheduleId}/partial-refund")
    public ResponseEntity<InstallmentResponse> partialRefund(
            @PathVariable String scheduleId,
            @RequestParam BigDecimal amount) {
        InstallmentResponse response = installmentService.processPartialRefund(scheduleId, amount);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{scheduleId}/early-settle")
    public ResponseEntity<InstallmentResponse> earlySettle(@PathVariable String scheduleId) {
        InstallmentResponse response = installmentService.earlySettle(scheduleId);
        return ResponseEntity.ok(response);
    }
}
