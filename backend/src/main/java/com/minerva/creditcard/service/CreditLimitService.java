package com.minerva.creditcard.service;

import com.minerva.creditcard.domain.*;
import com.minerva.creditcard.dto.*;
import com.minerva.creditcard.exception.*;
import com.minerva.creditcard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditLimitService {
    
    private final CreditLimitRepository creditLimitRepository;
    private final AccountRepository accountRepository;
    
    @Transactional(readOnly = true)
    public BigDecimal getAvailableCredit(Long accountId) {
        CreditLimit creditLimit = creditLimitRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditLimit", String.valueOf(accountId)));
        return creditLimit.getAvailableCreditLimit();
    }
    
    @Transactional(readOnly = true)
    public CreditLimit getCreditLimit(Long accountId) {
        return creditLimitRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditLimit", String.valueOf(accountId)));
    }
    
    @Transactional
    public CreditLimit adjustCreditLimit(Long accountId, BigDecimal newLimit) {
        log.info("Adjusting credit limit for account: {}, new limit: {}", accountId, newLimit);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", String.valueOf(accountId)));
        
        CreditLimit creditLimit = creditLimitRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditLimit", String.valueOf(accountId)));
        
        BigDecimal usedLimit = creditLimit.getUsedCreditLimit();
        BigDecimal limitDiff = newLimit.subtract(creditLimit.getTotalCreditLimit());
        
        creditLimit.setTotalCreditLimit(newLimit);
        creditLimit.setAvailableCreditLimit(newLimit.subtract(usedLimit));
        creditLimit = creditLimitRepository.save(creditLimit);
        
        account.setCreditLimit(newLimit);
        account.setAvailableCredit(newLimit.subtract(usedLimit));
        accountRepository.save(account);
        
        log.info("Credit limit adjusted successfully");
        
        return creditLimit;
    }
    
    @Transactional
    public CreditLimit setTemporaryLimit(Long accountId, BigDecimal amount, java.time.LocalDate expiryDate) {
        log.info("Setting temporary limit for account: {}, amount: {}, expiry: {}", 
                accountId, amount, expiryDate);
        
        CreditLimit creditLimit = creditLimitRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditLimit", String.valueOf(accountId)));
        
        creditLimit.setTemporaryLimit(amount);
        creditLimit.setTemporaryLimitExpiry(expiryDate);
        creditLimit = creditLimitRepository.save(creditLimit);
        
        log.info("Temporary limit set successfully");
        
        return creditLimit;
    }
}
