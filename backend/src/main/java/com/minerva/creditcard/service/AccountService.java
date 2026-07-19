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
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CreditLimitRepository creditLimitRepository;
    
    public AccountService(AccountRepository accountRepository, 
                         CardRepository cardRepository,
                         CreditLimitRepository creditLimitRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.creditLimitRepository = creditLimitRepository;
    }
    
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for customer: {}", request.getCustomerId());
        
        Account account = Account.builder()
                .accountNo(generateAccountNumber())
                .customerId(request.getCustomerId())
                .accountType(Account.AccountType.valueOf(request.getAccountType()))
                .status(Account.AccountStatus.PENDING)
                .creditLimit(request.getCreditLimit())
                .availableCredit(request.getCreditLimit())
                .currentBalance(BigDecimal.ZERO)
                .billingCycleDay(request.getBillingCycleDay() != null ? request.getBillingCycleDay() : 1)
                .build();
        
        account = accountRepository.save(account);
        
        // Create credit limit record
        CreditLimit creditLimit = CreditLimit.builder()
                .account(account)
                .totalCreditLimit(request.getCreditLimit())
                .availableCreditLimit(request.getCreditLimit())
                .usedCreditLimit(BigDecimal.ZERO)
                .cashAdvanceLimit(request.getCreditLimit().multiply(BigDecimal.valueOf(0.3)))
                .cashAdvanceUsed(BigDecimal.ZERO)
                .build();
        creditLimitRepository.save(creditLimit);
        
        // Activate account
        account.setStatus(Account.AccountStatus.ACTIVE);
        account = accountRepository.save(account);
        
        // Generate token for the card
        String token = generateToken();
        
        log.info("Account created successfully: {}", account.getAccountNo());
        
        return AccountResponse.builder()
                .id(account.getId())
                .accountNo(account.getAccountNo())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .status(account.getStatus().name())
                .creditLimit(account.getCreditLimit())
                .availableCredit(account.getAvailableCredit())
                .currentBalance(account.getCurrentBalance())
                .billingCycleDay(account.getBillingCycleDay())
                .token(token)
                .createdAt(account.getCreatedAt())
                .build();
    }
    
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNo) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNo));
        
        return toAccountResponse(account);
    }
    
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", String.valueOf(accountId)));
        
        return toAccountResponse(account);
    }
    
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toAccountResponse)
                .toList();
    }
    
    @Transactional
    public AccountResponse updateAccountStatus(String accountNo, String status) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNo));
        
        account.setStatus(Account.AccountStatus.valueOf(status));
        account = accountRepository.save(account);
        
        return toAccountResponse(account);
    }
    
    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNo(account.getAccountNo())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .status(account.getStatus().name())
                .creditLimit(account.getCreditLimit())
                .availableCredit(account.getAvailableCredit())
                .currentBalance(account.getCurrentBalance())
                .billingCycleDay(account.getBillingCycleDay())
                .createdAt(account.getCreatedAt())
                .build();
    }
    
    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
    
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
