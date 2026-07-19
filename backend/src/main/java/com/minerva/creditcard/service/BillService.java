package com.minerva.creditcard.service;

import com.minerva.creditcard.domain.*;
import com.minerva.creditcard.dto.*;
import com.minerva.creditcard.exception.*;
import com.minerva.creditcard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class BillService {
    
    private static final Logger log = LoggerFactory.getLogger(BillService.class);
    
    private final BillRepository billRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    
    private static final BigDecimal MIN_PAYMENT_PERCENTAGE = new BigDecimal("0.05");
    private static final BigDecimal DEFAULT_APR = new BigDecimal("0.18");
    
    public BillService(BillRepository billRepository,
                       AccountRepository accountRepository,
                       TransactionRepository transactionRepository) {
        this.billRepository = billRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }
    
    @Transactional
    public Bill generateBill(String accountNo, LocalDate billingPeriodEnd) {
        log.info("Generating bill for account: {}, period ending: {}", accountNo, billingPeriodEnd);
        
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNo));
        
        // Calculate billing period start (previous billing cycle date or 30 days ago)
        LocalDate billingPeriodStart = billingPeriodEnd.minusDays(30);
        
        // Check if bill already exists for this period
        if (billRepository.findByAccountNoAndStatementDateBetween(
                accountNo, billingPeriodStart, billingPeriodEnd).isPresent()) {
            throw new BusinessException("Bill already exists for this period");
        }
        
        LocalDate statementDate = billingPeriodEnd;
        LocalDate paymentDueDate = billingPeriodEnd.plusDays(20); // 20 days grace period
        
        // Get transactions for billing period
        LocalDateTime startDateTime = billingPeriodStart.atStartOfDay();
        LocalDateTime endDateTime = billingPeriodEnd.atTime(LocalTime.MAX);
        
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
                account.getId(), startDateTime, endDateTime);
        
        // Calculate bill amounts
        BigDecimal newCharges = transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.CAPTURE ||
                            t.getTransactionType() == Transaction.TransactionType.AUTHORIZATION)
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.SETTLED ||
                            t.getStatus() == Transaction.TransactionStatus.APPROVED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal paymentsRefunds = transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.PAYMENT ||
                            t.getTransactionType() == Transaction.TransactionType.REFUND)
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.APPROVED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal previousBalance = account.getCurrentBalance().subtract(newCharges).add(paymentsRefunds);
        BigDecimal totalAmount = previousBalance.add(newCharges);
        BigDecimal minimumPayment = calculateMinimumPayment(totalAmount);
        
        String billId = UUID.randomUUID().toString();
        
        Bill bill = Bill.builder()
                .billId(billId)
                .account(account)
                .billingPeriodStart(billingPeriodStart)
                .billingPeriodEnd(billingPeriodEnd)
                .statementDate(statementDate)
                .paymentDueDate(paymentDueDate)
                .totalAmount(totalAmount)
                .minimumPayment(minimumPayment)
                .previousBalance(previousBalance)
                .newCharges(newCharges)
                .paymentsRefunds(paymentsRefunds)
                .adjustments(BigDecimal.ZERO)
                .interestCharges(BigDecimal.ZERO)
                .apr(DEFAULT_APR)
                .status(Bill.BillStatus.ISSUED)
                .build();
        
        // Create bill items from transactions
        for (Transaction tx : transactions) {
            BillItem item = BillItem.builder()
                    .bill(bill)
                    .transaction(tx)
                    .description(tx.getDescription())
                    .transactionDate(tx.getCreatedAt())
                    .amount(tx.getAmount())
                    .itemType(mapTransactionTypeToBillItemType(tx.getTransactionType()))
                    .isInstallment(tx.getInstallmentSchedule() != null)
                    .build();
            
            if (tx.getInstallmentSchedule() != null) {
                item.setInstallmentNumber(tx.getInstallmentSchedule().getInstallmentsRemaining());
                item.setTotalInstallments(tx.getInstallmentSchedule().getInstallmentCount());
            }
            
            bill.getItems().add(item);
        }
        
        bill = billRepository.save(bill);
        
        log.info("Bill generated: {}", billId);
        
        return bill;
    }
    
    @Transactional(readOnly = true)
    public BillResponse getBill(String billId) {
        Bill bill = billRepository.findByBillId(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", billId));
        return toBillResponse(bill);
    }
    
    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByAccount(Long accountId) {
        return billRepository.findByAccountIdOrderByStatementDateDesc(accountId)
                .stream()
                .map(this::toBillResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<BillResponse> getUnpaidBills(Long accountId) {
        List<Bill.BillStatus> unpaidStatuses = List.of(
                Bill.BillStatus.ISSUED,
                Bill.BillStatus.OVERDUE,
                Bill.BillStatus.PARTIALLY_PAID
        );
        return billRepository.findByStatusIn(unpaidStatuses)
                .stream()
                .filter(b -> b.getAccount().getId().equals(accountId))
                .map(this::toBillResponse)
                .toList();
    }
    
    private BigDecimal calculateMinimumPayment(BigDecimal totalAmount) {
        BigDecimal minPayment = totalAmount.multiply(MIN_PAYMENT_PERCENTAGE);
        // Minimum payment should be at least 50 CNY
        if (minPayment.compareTo(new BigDecimal("50")) < 0) {
            return totalAmount.min(new BigDecimal("50"));
        }
        return minPayment;
    }
    
    private BillItem.BillItemType mapTransactionTypeToBillItemType(Transaction.TransactionType txType) {
        return switch (txType) {
            case AUTHORIZATION, CAPTURE -> BillItem.BillItemType.PURCHASE;
            case REFUND -> BillItem.BillItemType.REFUND;
            case PAYMENT -> BillItem.BillItemType.PAYMENT;
            case REVERSAL -> BillItem.BillItemType.FEE;
        };
    }
    
    private BillResponse toBillResponse(Bill bill) {
        List<BillItemResponse> items = bill.getItems().stream()
                .map(item -> BillItemResponse.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .transactionDate(item.getTransactionDate())
                        .amount(item.getAmount())
                        .itemType(item.getItemType().name())
                        .isInstallment(item.getIsInstallment())
                        .installmentNumber(item.getInstallmentNumber())
                        .totalInstallments(item.getTotalInstallments())
                        .build())
                .toList();
        
        return BillResponse.builder()
                .billId(bill.getBillId())
                .accountId(bill.getAccount().getId())
                .billingPeriodStart(bill.getBillingPeriodStart())
                .billingPeriodEnd(bill.getBillingPeriodEnd())
                .statementDate(bill.getStatementDate())
                .paymentDueDate(bill.getPaymentDueDate())
                .totalAmount(bill.getTotalAmount())
                .minimumPayment(bill.getMinimumPayment())
                .status(bill.getStatus().name())
                .items(items)
                .build();
    }
}
