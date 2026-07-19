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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private CardRepository cardRepository;
    
    @Mock
    private CreditLimitRepository creditLimitRepository;
    
    @Mock
    private InstallmentScheduleRepository installmentScheduleRepository;
    
    @InjectMocks
    private TransactionService transactionService;
    
    @Test
    void authorize_Success() {
        // Given
        AuthorizationRequest request = AuthorizationRequest.builder()
                .token("test-token-123")
                .amount(new BigDecimal("1000"))
                .currency("CNY")
                .merchantName("Test Merchant")
                .merchantCategoryCode("5411")
                .build();
        
        Card card = Card.builder()
                .id(1L)
                .cardId("CARD-001")
                .token("test-token-123")
                .status(Card.CardStatus.ACTIVE)
                .build();
        
        Account account = Account.builder()
                .id(1L)
                .accountNo("ACC-001")
                .customerId(1001L)
                .accountType(Account.AccountType.CREDIT_CARD)
                .status(Account.AccountStatus.ACTIVE)
                .build();
        card.setAccount(account);
        
        CreditLimit creditLimit = CreditLimit.builder()
                .account(account)
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("49000"))
                .usedCreditLimit(new BigDecimal("1000"))
                .build();
        
        when(cardRepository.findByToken("test-token-123")).thenReturn(Optional.of(card));
        when(accountRepository.findByAccountNoForUpdate("ACC-001")).thenReturn(Optional.of(account));
        when(creditLimitRepository.findByAccountIdForUpdate(1L)).thenReturn(Optional.of(creditLimit));
        when(creditLimitRepository.save(any(CreditLimit.class))).thenReturn(creditLimit);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
        AuthorizationResponse response = transactionService.authorize(request);
        
        // Then
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertNotNull(response.getAuthorizationCode());
        assertEquals(new BigDecimal("1000"), response.getAuthorizedAmount());
        
        verify(creditLimitRepository).useCredit(any(BigDecimal.class));
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void authorize_InsufficientCredit() {
        // Given
        AuthorizationRequest request = AuthorizationRequest.builder()
                .token("test-token-123")
                .amount(new BigDecimal("100000"))
                .currency("CNY")
                .build();
        
        Card card = Card.builder()
                .id(1L)
                .cardId("CARD-001")
                .token("test-token-123")
                .status(Card.CardStatus.ACTIVE)
                .build();
        
        Account account = Account.builder()
                .id(1L)
                .accountNo("ACC-001")
                .status(Account.AccountStatus.ACTIVE)
                .build();
        card.setAccount(account);
        
        CreditLimit creditLimit = CreditLimit.builder()
                .account(account)
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("10000"))
                .usedCreditLimit(new BigDecimal("40000"))
                .build();
        
        when(cardRepository.findByToken("test-token-123")).thenReturn(Optional.of(card));
        when(accountRepository.findByAccountNoForUpdate("ACC-001")).thenReturn(Optional.of(account));
        when(creditLimitRepository.findByAccountIdForUpdate(1L)).thenReturn(Optional.of(creditLimit));
        
        // When
        AuthorizationResponse response = transactionService.authorize(request);
        
        // Then
        assertEquals("DECLINED", response.getStatus());
        assertEquals("Insufficient credit limit", response.getMessage());
    }
    
    @Test
    void authorize_CardNotActive() {
        // Given
        AuthorizationRequest request = AuthorizationRequest.builder()
                .token("test-token-123")
                .amount(new BigDecimal("1000"))
                .build();
        
        Card card = Card.builder()
                .id(1L)
                .cardId("CARD-001")
                .token("test-token-123")
                .status(Card.CardStatus.SUSPENDED)
                .build();
        
        when(cardRepository.findByToken("test-token-123")).thenReturn(Optional.of(card));
        
        // When
        AuthorizationResponse response = transactionService.authorize(request);
        
        // Then
        assertEquals("DECLINED", response.getStatus());
        assertEquals("Card is not active", response.getMessage());
    }
    
    @Test
    void processPayment_Success() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .accountNo("ACC-001")
                .amount(new BigDecimal("500"))
                .currency("CNY")
                .build();
        
        Account account = Account.builder()
                .id(1L)
                .accountNo("ACC-001")
                .currentBalance(new BigDecimal("1000"))
                .status(Account.AccountStatus.ACTIVE)
                .build();
        
        when(accountRepository.findByAccountNoForUpdate("ACC-001")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
        TransactionResponse response = transactionService.processPayment(request);
        
        // Then
        assertNotNull(response);
        assertEquals("PAYMENT", response.getTransactionType());
        assertEquals("APPROVED", response.getStatus());
        assertEquals(new BigDecimal("500"), response.getAmount());
    }
    
    @Test
    void refund_Success() {
        // Given
        RefundRequest request = RefundRequest.builder()
                .originalTransactionId("TXN-001")
                .amount(new BigDecimal("200"))
                .reason("Customer return")
                .build();
        
        Transaction originalTx = Transaction.builder()
                .id(1L)
                .transactionId("TXN-001")
                .transactionType(Transaction.TransactionType.CAPTURE)
                .amount(new BigDecimal("500"))
                .currency("CNY")
                .status(Transaction.TransactionStatus.SETTLED)
                .build();
        
        Account account = Account.builder()
                .id(1L)
                .accountNo("ACC-001")
                .currentBalance(new BigDecimal("500"))
                .status(Account.AccountStatus.ACTIVE)
                .build();
        originalTx.setAccount(account);
        
        CreditLimit creditLimit = CreditLimit.builder()
                .account(account)
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("45000"))
                .usedCreditLimit(new BigDecimal("5000"))
                .build();
        
        when(transactionRepository.findByTransactionIdForUpdate("TXN-001")).thenReturn(Optional.of(originalTx));
        when(creditLimitRepository.findByAccountIdForUpdate(1L)).thenReturn(Optional.of(creditLimit));
        when(creditLimitRepository.save(any(CreditLimit.class))).thenReturn(creditLimit);
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
        TransactionResponse response = transactionService.refund(request);
        
        // Then
        assertNotNull(response);
        assertEquals("REFUND", response.getTransactionType());
        assertEquals(new BigDecimal("200"), response.getAmount());
        verify(creditLimitRepository).releaseCredit(any(BigDecimal.class));
    }
}
