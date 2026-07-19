package com.minerva.creditcard.service;

import com.minerva.creditcard.domain.*;
import com.minerva.creditcard.dto.*;
import com.minerva.creditcard.exception.*;
import com.minerva.creditcard.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {
    
    @Mock
    private BillRepository billRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private BillService billService;
    
    @Test
    void generateBill_Success() {
        // Given
        String accountNo = "ACC-001";
        LocalDate billingPeriodEnd = LocalDate.of(2024, 1, 31);
        
        Account account = Account.builder()
                .id(1L)
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("5000"))
                .status(Account.AccountStatus.ACTIVE)
                .billingCycleDay(31)
                .build();
        
        List<Transaction> transactions = new ArrayList<>();
        
        // Add purchase transaction
        Transaction purchase = Transaction.builder()
                .id(1L)
                .transactionId("TXN-001")
                .account(account)
                .transactionType(Transaction.TransactionType.CAPTURE)
                .amount(new BigDecimal("2000"))
                .status(Transaction.TransactionStatus.SETTLED)
                .description("Purchase at Store")
                .createdAt(LocalDateTime.of(2024, 1, 15, 10, 0))
                .build();
        transactions.add(purchase);
        
        // Add payment transaction
        Transaction payment = Transaction.builder()
                .id(2L)
                .transactionId("TXN-002")
                .account(account)
                .transactionType(Transaction.TransactionType.PAYMENT)
                .amount(new BigDecimal("1000"))
                .status(Transaction.TransactionStatus.APPROVED)
                .description("Payment received")
                .createdAt(LocalDateTime.of(2024, 1, 20, 14, 0))
                .build();
        transactions.add(payment);
        
        when(accountRepository.findByAccountNo(accountNo)).thenReturn(Optional.of(account));
        when(billRepository.findByAccountNoAndStatementDateBetween(eq(accountNo), any(), any()))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByAccountIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(transactions);
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> {
            Bill b = i.getArgument(0);
            b.setId(1L);
            return b;
        });
        
        // When
        Bill bill = billService.generateBill(accountNo, billingPeriodEnd);
        
        // Then
        assertNotNull(bill);
        assertEquals(new BigDecimal("6000"), bill.getTotalAmount()); // 5000 + 2000 - 1000
        assertTrue(bill.getNewCharges().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(bill.getMinimumPayment().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(Bill.BillStatus.ISSUED, bill.getStatus());
        
        verify(billRepository).save(any(Bill.class));
    }
    
    @Test
    void generateBill_BillAlreadyExists() {
        // Given
        String accountNo = "ACC-001";
        LocalDate billingPeriodEnd = LocalDate.of(2024, 1, 31);
        
        Account account = Account.builder()
                .id(1L)
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("5000"))
                .status(Account.AccountStatus.ACTIVE)
                .build();
        
        when(accountRepository.findByAccountNo(accountNo)).thenReturn(Optional.of(account));
        when(billRepository.findByAccountNoAndStatementDateBetween(eq(accountNo), any(), any()))
                .thenReturn(Optional.of(new Bill()));
        
        // When & Then
        assertThrows(BusinessException.class, () -> {
            billService.generateBill(accountNo, billingPeriodEnd);
        });
    }
    
    @Test
    void getBillsByAccount_Success() {
        // Given
        Long accountId = 1L;
        
        Account account = Account.builder()
                .id(accountId)
                .accountNo("ACC-001")
                .build();
        
        List<Bill> bills = new ArrayList<>();
        
        Bill bill1 = Bill.builder()
                .id(1L)
                .billId("BILL-001")
                .account(account)
                .billingPeriodStart(LocalDate.of(2024, 1, 1))
                .billingPeriodEnd(LocalDate.of(2024, 1, 31))
                .statementDate(LocalDate.of(2024, 1, 31))
                .paymentDueDate(LocalDate.of(2024, 2, 20))
                .totalAmount(new BigDecimal("5000"))
                .minimumPayment(new BigDecimal("250"))
                .status(Bill.BillStatus.PAID)
                .items(new ArrayList<>())
                .build();
        bills.add(bill1);
        
        when(billRepository.findByAccountIdOrderByStatementDateDesc(accountId)).thenReturn(bills);
        
        // When
        List<BillResponse> responses = billService.getBillsByAccount(accountId);
        
        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("BILL-001", responses.get(0).getBillId());
        assertEquals("PAID", responses.get(0).getStatus());
    }
    
    @Test
    void minimumPaymentCalculation() {
        // Test minimum payment calculation logic
        // Minimum payment is 5% of total, but at least 50 CNY
        
        // Given
        BigDecimal total5000 = new BigDecimal("5000");
        BigDecimal total100 = new BigDecimal("100");
        BigDecimal total30 = new BigDecimal("30");
        
        // When
        BigDecimal minPayment5000 = calculateMinimumPayment(total5000);
        BigDecimal minPayment100 = calculateMinimumPayment(total100);
        BigDecimal minPayment30 = calculateMinimumPayment(total30);
        
        // Then
        assertEquals(new BigDecimal("250.00"), minPayment5000); // 5% of 5000
        assertEquals(new BigDecimal("50.00"), minPayment100); // 5% is 5, but min is 50
        assertEquals(new BigDecimal("30.00"), minPayment30); // Less than 50, use total
    }
    
    private BigDecimal calculateMinimumPayment(BigDecimal totalAmount) {
        BigDecimal MIN_PAYMENT_PERCENTAGE = new BigDecimal("0.05");
        BigDecimal minPayment = totalAmount.multiply(MIN_PAYMENT_PERCENTAGE);
        if (minPayment.compareTo(new BigDecimal("50")) < 0) {
            return totalAmount.min(new BigDecimal("50"));
        }
        return minPayment;
    }
}
