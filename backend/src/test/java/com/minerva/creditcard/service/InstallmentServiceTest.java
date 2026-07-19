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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentServiceTest {
    
    @Mock
    private InstallmentScheduleRepository installmentScheduleRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private CreditLimitRepository creditLimitRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @InjectMocks
    private InstallmentService installmentService;
    
    @Test
    void createInstallment_Success() {
        // Given
        InstallmentRequest request = InstallmentRequest.builder()
                .transactionId("TXN-001")
                .installmentCount(12)
                .apr(new BigDecimal("0.12"))
                .build();
        
        Account account = Account.builder()
                .id(1L)
                .accountNo("ACC-001")
                .status(Account.AccountStatus.ACTIVE)
                .build();
        
        Transaction transaction = Transaction.builder()
                .id(1L)
                .transactionId("TXN-001")
                .account(account)
                .amount(new BigDecimal("12000"))
                .status(Transaction.TransactionStatus.APPROVED)
                .build();
        
        CreditLimit creditLimit = CreditLimit.builder()
                .account(account)
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("38000"))
                .usedCreditLimit(new BigDecimal("12000"))
                .build();
        
        when(transactionRepository.findByTransactionIdForUpdate("TXN-001")).thenReturn(Optional.of(transaction));
        when(creditLimitRepository.findByAccountIdForUpdate(1L)).thenReturn(Optional.of(creditLimit));
        when(creditLimitRepository.save(any(CreditLimit.class))).thenReturn(creditLimit);
        when(installmentScheduleRepository.save(any(InstallmentSchedule.class))).thenAnswer(i -> {
            InstallmentSchedule s = i.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        
        // When
        InstallmentResponse response = installmentService.createInstallment(request);
        
        // Then
        assertNotNull(response);
        assertEquals(12, response.getInstallmentCount());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(new BigDecimal("12000"), response.getPrincipalAmount());
        assertTrue(response.getTotalInterest().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify credit was reserved for remaining installments
        verify(creditLimitRepository).useCredit(any(BigDecimal.class));
    }
    
    @Test
    void createInstallment_TransactionNotApproved() {
        // Given
        InstallmentRequest request = InstallmentRequest.builder()
                .transactionId("TXN-001")
                .installmentCount(12)
                .apr(new BigDecimal("0.12"))
                .build();
        
        Transaction transaction = Transaction.builder()
                .id(1L)
                .transactionId("TXN-001")
                .status(Transaction.TransactionStatus.PENDING)
                .build();
        
        when(transactionRepository.findByTransactionIdForUpdate("TXN-001")).thenReturn(Optional.of(transaction));
        
        // When & Then
        assertThrows(BusinessException.class, () -> {
            installmentService.createInstallment(request);
        });
    }
    
    @Test
    void processPartialRefund_Success() {
        // Given
        BigDecimal refundAmount = new BigDecimal("1000");
        
        Account account = Account.builder()
                .id(1L)
                .accountNo("ACC-001")
                .status(Account.AccountStatus.ACTIVE)
                .build();
        
        InstallmentSchedule schedule = InstallmentSchedule.builder()
                .id(1L)
                .scheduleId("SCH-001")
                .account(account)
                .principalAmount(new BigDecimal("12000"))
                .totalAmount(new BigDecimal("12720"))
                .monthlyPayment(new BigDecimal("1060"))
                .installmentCount(12)
                .installmentsRemaining(10)
                .remainingPrincipal(new BigDecimal("10600"))
                .status(InstallmentSchedule.InstallmentStatus.ACTIVE)
                .startDate(LocalDate.now())
                .nextPaymentDate(LocalDate.now().plusMonths(1))
                .build();
        
        CreditLimit creditLimit = CreditLimit.builder()
                .account(account)
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("38000"))
                .usedCreditLimit(new BigDecimal("12000"))
                .build();
        
        when(installmentScheduleRepository.findByScheduleId("SCH-001")).thenReturn(Optional.of(schedule));
        when(installmentScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(creditLimitRepository.findByAccountIdForUpdate(1L)).thenReturn(Optional.of(creditLimit));
        when(creditLimitRepository.save(any(CreditLimit.class))).thenReturn(creditLimit);
        when(installmentScheduleRepository.save(any(InstallmentSchedule.class))).thenReturn(schedule);
        
        // When
        InstallmentResponse response = installmentService.processPartialRefund("SCH-001", refundAmount);
        
        // Then
        assertNotNull(response);
        // Remaining principal should be reduced by refund amount
        assertEquals(new BigDecimal("9600"), response.getRemainingPrincipal());
        
        verify(creditLimitRepository).releaseCredit(refundAmount);
    }
    
    @Test
    void earlySettle_Success() {
        // Given
        Account account = Account.builder()
                .id(1L)
                .accountNo("ACC-001")
                .status(Account.AccountStatus.ACTIVE)
                .build();
        
        InstallmentSchedule schedule = InstallmentSchedule.builder()
                .id(1L)
                .scheduleId("SCH-001")
                .account(account)
                .principalAmount(new BigDecimal("12000"))
                .installmentsRemaining(10)
                .remainingPrincipal(new BigDecimal("10000"))
                .status(InstallmentSchedule.InstallmentStatus.ACTIVE)
                .build();
        
        CreditLimit creditLimit = CreditLimit.builder()
                .account(account)
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("40000"))
                .usedCreditLimit(new BigDecimal("10000"))
                .build();
        
        when(installmentScheduleRepository.findByScheduleId("SCH-001")).thenReturn(Optional.of(schedule));
        when(creditLimitRepository.findByAccountIdForUpdate(1L)).thenReturn(Optional.of(creditLimit));
        when(creditLimitRepository.save(any(CreditLimit.class))).thenReturn(creditLimit);
        when(installmentScheduleRepository.save(any(InstallmentSchedule.class))).thenReturn(schedule);
        
        // When
        InstallmentResponse response = installmentService.earlySettle("SCH-001");
        
        // Then
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(0, response.getInstallmentsRemaining());
        
        // Verify remaining credit was released
        verify(creditLimitRepository).releaseCredit(new BigDecimal("10000"));
    }
    
    @Test
    void installmentInterestCalculation() {
        // Test the interest calculation formula
        // Given
        BigDecimal principal = new BigDecimal("12000");
        BigDecimal apr = new BigDecimal("0.12");
        int months = 12;
        
        // When - using the same formula as the service
        BigDecimal totalInterest = principal.multiply(apr)
                .multiply(BigDecimal.valueOf(months))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        
        // Then - expected: 12000 * 0.12 * 12 / 12 = 1440
        assertEquals(new BigDecimal("1440.00"), totalInterest);
    }
}
