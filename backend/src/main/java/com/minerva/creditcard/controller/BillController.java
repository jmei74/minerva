package com.minerva.creditcard.controller;

import com.minerva.creditcard.dto.*;
import com.minerva.creditcard.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillController {
    
    private final BillService billService;
    
    @PostMapping("/generate")
    public ResponseEntity<BillResponse> generateBill(
            @RequestParam String accountNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingPeriodEnd) {
        Bill bill = billService.generateBill(accountNo, billingPeriodEnd);
        BillResponse response = billService.getBill(bill.getBillId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{billId}")
    public ResponseEntity<BillResponse> getBill(@PathVariable String billId) {
        BillResponse response = billService.getBill(billId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<BillResponse>> getBillsByAccount(@PathVariable Long accountId) {
        List<BillResponse> responses = billService.getBillsByAccount(accountId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/account/{accountId}/unpaid")
    public ResponseEntity<List<BillResponse>> getUnpaidBills(@PathVariable Long accountId) {
        List<BillResponse> responses = billService.getUnpaidBills(accountId);
        return ResponseEntity.ok(responses);
    }
}
