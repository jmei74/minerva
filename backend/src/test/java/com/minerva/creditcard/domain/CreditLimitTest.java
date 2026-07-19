package com.minerva.creditcard.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CreditLimitTest {
    
    @Test
    void hasAvailableCredit_WithinLimit() {
        // Given
        CreditLimit creditLimit = CreditLimit.builder()
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("10000"))
                .usedCreditLimit(new BigDecimal("40000"))
                .build();
        
        // When & Then
        assertTrue(creditLimit.hasAvailableCredit(new BigDecimal("5000")));
        assertTrue(creditLimit.hasAvailableCredit(new BigDecimal("10000")));
        assertFalse(creditLimit.hasAvailableCredit(new BigDecimal("10001")));
    }
    
    @Test
    void hasAvailableCredit_WithTemporaryLimit() {
        // Given
        CreditLimit creditLimit = CreditLimit.builder()
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("10000"))
                .usedCreditLimit(new BigDecimal("40000"))
                .temporaryLimit(new BigDecimal("10000"))
                .temporaryLimitExpiry(LocalDate.now().plusDays(30))
                .build();
        
        // When & Then - temporary limit should be considered
        // Note: The hasAvailableCredit method uses availableCreditLimit directly
        // The temporary limit is meant to extend available credit temporarily
        assertTrue(creditLimit.hasAvailableCredit(new BigDecimal("15000")));
    }
    
    @Test
    void useCredit_Success() {
        // Given
        CreditLimit creditLimit = CreditLimit.builder()
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("10000"))
                .usedCreditLimit(new BigDecimal("40000"))
                .build();
        
        // When
        creditLimit.useCredit(new BigDecimal("5000"));
        
        // Then
        assertEquals(0, new BigDecimal("5000").compareTo(creditLimit.getAvailableCreditLimit()));
        assertEquals(0, new BigDecimal("45000").compareTo(creditLimit.getUsedCreditLimit()));
    }
    
    @Test
    void useCredit_InsufficientLimit() {
        // Given
        CreditLimit creditLimit = CreditLimit.builder()
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("10000"))
                .usedCreditLimit(new BigDecimal("40000"))
                .build();
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            creditLimit.useCredit(new BigDecimal("15000"));
        });
    }
    
    @Test
    void releaseCredit_Success() {
        // Given
        CreditLimit creditLimit = CreditLimit.builder()
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("30000"))
                .usedCreditLimit(new BigDecimal("20000"))
                .build();
        
        // When
        creditLimit.releaseCredit(new BigDecimal("5000"));
        
        // Then
        assertEquals(0, new BigDecimal("35000").compareTo(creditLimit.getAvailableCreditLimit()));
        assertEquals(0, new BigDecimal("15000").compareTo(creditLimit.getUsedCreditLimit()));
    }
    
    @Test
    void releaseCredit_CannotGoBelowZero() {
        // Given
        CreditLimit creditLimit = CreditLimit.builder()
                .totalCreditLimit(new BigDecimal("50000"))
                .availableCreditLimit(new BigDecimal("10000"))
                .usedCreditLimit(new BigDecimal("40000"))
                .build();
        
        // When
        creditLimit.releaseCredit(new BigDecimal("50000"));
        
        // Then - used limit should be 0
        assertTrue(creditLimit.getUsedCreditLimit().compareTo(BigDecimal.ZERO) == 0);
        assertTrue(creditLimit.getAvailableCreditLimit().compareTo(new BigDecimal("60000")) == 0);
    }
}
