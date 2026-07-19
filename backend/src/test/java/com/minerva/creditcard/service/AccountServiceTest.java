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
class AccountServiceTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private CardRepository cardRepository;
    
    @Mock
    private CreditLimitRepository creditLimitRepository;
    
    @InjectMocks
    private AccountService accountService;
    
    @Test
    void createAccount_Success() {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerId(1001L)
                .accountType("CREDIT_CARD")
                .creditLimit(new BigDecimal("50000"))
                .billingCycleDay(15)
                .build();
        
        Account savedAccount = Account.builder()
                .id(1L)
                .accountNo("ACC123456")
                .customerId(1001L)
                .accountType(Account.AccountType.CREDIT_CARD)
                .status(Account.AccountStatus.ACTIVE)
                .creditLimit(new BigDecimal("50000"))
                .availableCredit(new BigDecimal("50000"))
                .currentBalance(BigDecimal.ZERO)
                .billingCycleDay(15)
                .build();
        
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(creditLimitRepository.save(any(CreditLimit.class))).thenReturn(new CreditLimit());
        
        // When
        AccountResponse response = accountService.createAccount(request);
        
        // Then
        assertNotNull(response);
        assertEquals(1001L, response.getCustomerId());
        assertEquals("CREDIT_CARD", response.getAccountType());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(new BigDecimal("50000"), response.getCreditLimit());
        assertNotNull(response.getToken());
        
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(creditLimitRepository).save(any(CreditLimit.class));
    }
    
    @Test
    void getAccount_NotFound() {
        // Given
        String accountNo = "NONEXISTENT";
        when(accountRepository.findByAccountNo(accountNo)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            accountService.getAccount(accountNo);
        });
    }
    
    @Test
    void updateAccountStatus_Success() {
        // Given
        String accountNo = "ACC123456";
        Account account = Account.builder()
                .id(1L)
                .accountNo(accountNo)
                .customerId(1001L)
                .accountType(Account.AccountType.CREDIT_CARD)
                .status(Account.AccountStatus.ACTIVE)
                .creditLimit(new BigDecimal("50000"))
                .availableCredit(new BigDecimal("50000"))
                .currentBalance(BigDecimal.ZERO)
                .billingCycleDay(1)
                .build();
        
        when(accountRepository.findByAccountNo(accountNo)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        // When
        AccountResponse response = accountService.updateAccountStatus(accountNo, "SUSPENDED");
        
        // Then
        assertEquals("SUSPENDED", response.getStatus());
        verify(accountRepository).save(any(Account.class));
    }
}
