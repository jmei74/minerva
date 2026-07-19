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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CreditLimitRepository creditLimitRepository;
    private final InstallmentScheduleRepository installmentScheduleRepository;
    
    @Transactional
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        log.info("Processing authorization for token: {}", request.getToken());
        
        Card card = cardRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Card", request.getToken()));
        
        if (card.getStatus() != Card.CardStatus.ACTIVE) {
            return AuthorizationResponse.builder()
                    .status("DECLINED")
                    .message("Card is not active")
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        
        Account account = accountRepository.findByAccountNoForUpdate(card.getAccount().getAccountNo())
                .orElseThrow(() -> new ResourceNotFoundException("Account", card.getAccount().getAccountNo()));
        
        CreditLimit creditLimit = creditLimitRepository.findByAccountIdForUpdate(account.getId())
                .orElseThrow(() -> new BusinessException("Credit limit not found"));
        
        if (!creditLimit.hasAvailableCredit(request.getAmount())) {
            log.warn("Insufficient credit for account: {}, requested: {}, available: {}", 
                    account.getAccountNo(), request.getAmount(), creditLimit.getAvailableCreditLimit());
            return AuthorizationResponse.builder()
                    .status("DECLINED")
                    .message("Insufficient credit limit")
                    .availableCredit(creditLimit.getAvailableCreditLimit())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        
        // Reserve credit
        creditLimit.useCredit(request.getAmount());
        creditLimitRepository.save(creditLimit);
        
        // Create authorization transaction
        String transactionId = UUID.randomUUID().toString();
        String authCode = generateAuthorizationCode();
        
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .account(account)
                .transactionType(Transaction.TransactionType.AUTHORIZATION)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "CNY")
                .merchantName(request.getMerchantName())
                .merchantCategoryCode(request.getMerchantCategoryCode())
                .status(Transaction.TransactionStatus.APPROVED)
                .authorizationCode(authCode)
                .description(request.getDescription())
                .installmentCount(request.getInstallmentCount())
                .build();
        
        transactionRepository.save(transaction);
        
        log.info("Authorization approved: {}, auth code: {}", transactionId, authCode);
        
        return AuthorizationResponse.builder()
                .transactionId(transactionId)
                .status("APPROVED")
                .authorizationCode(authCode)
                .authorizedAmount(request.getAmount())
                .availableCredit(creditLimit.getAvailableCreditLimit())
                .message("Authorization approved")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    @Transactional
    public TransactionResponse capture(String transactionId) {
        log.info("Capturing transaction: {}", transactionId);
        
        Transaction transaction = transactionRepository.findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.APPROVED) {
            throw new BusinessException("Transaction cannot be captured, status: " + transaction.getStatus());
        }
        
        transaction.setStatus(Transaction.TransactionStatus.SETTLED);
        transaction = transactionRepository.save(transaction);
        
        // Update account balance
        Account account = transaction.getAccount();
        account.setCurrentBalance(account.getCurrentBalance().add(transaction.getAmount()));
        accountRepository.save(account);
        
        log.info("Transaction captured: {}", transactionId);
        
        return toTransactionResponse(transaction);
    }
    
    @Transactional
    public TransactionResponse refund(RefundRequest request) {
        log.info("Processing refund for transaction: {}", request.getOriginalTransactionId());
        
        Transaction originalTx = transactionRepository.findByTransactionIdForUpdate(request.getOriginalTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", request.getOriginalTransactionId()));
        
        BigDecimal refundAmount = request.getAmount();
        if (refundAmount.compareTo(originalTx.getAmount()) > 0) {
            throw new BusinessException("Refund amount exceeds original transaction amount");
        }
        
        // Create refund transaction
        String refundTxId = UUID.randomUUID().toString();
        
        Transaction refundTx = Transaction.builder()
                .transactionId(refundTxId)
                .account(originalTx.getAccount())
                .transactionType(Transaction.TransactionType.REFUND)
                .amount(refundAmount)
                .currency(originalTx.getCurrency())
                .status(Transaction.TransactionStatus.APPROVED)
                .referenceId(originalTx.getTransactionId())
                .description(request.getReason())
                .build();
        
        refundTx = transactionRepository.save(refundTx);
        
        // Release credit
        CreditLimit creditLimit = creditLimitRepository.findByAccountIdForUpdate(originalTx.getAccount().getId())
                .orElseThrow(() -> new BusinessException("Credit limit not found"));
        
        creditLimit.releaseCredit(refundAmount);
        creditLimitRepository.save(creditLimit);
        
        // Update account balance
        Account account = originalTx.getAccount();
        account.setCurrentBalance(account.getCurrentBalance().subtract(refundAmount));
        accountRepository.save(account);
        
        // Handle installment refund if applicable
        if (originalTx.getInstallmentSchedule() != null && refundAmount.compareTo(originalTx.getAmount()) == 0) {
            // Full refund - cancel installment
            InstallmentSchedule schedule = originalTx.getInstallmentSchedule();
            schedule.setStatus(InstallmentSchedule.InstallmentStatus.CANCELLED);
            installmentScheduleRepository.save(schedule);
            
            creditLimit.releaseCredit(schedule.getRemainingPrincipal());
            creditLimitRepository.save(creditLimit);
        }
        
        log.info("Refund processed: {}", refundTxId);
        
        return toTransactionResponse(refundTx);
    }
    
    @Transactional
    public TransactionResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for account: {}", request.getAccountNo());
        
        Account account = accountRepository.findByAccountNoForUpdate(request.getAccountNo())
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.getAccountNo()));
        
        String paymentTxId = UUID.randomUUID().toString();
        
        Transaction paymentTx = Transaction.builder()
                .transactionId(paymentTxId)
                .account(account)
                .transactionType(Transaction.TransactionType.PAYMENT)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "CNY")
                .status(Transaction.TransactionStatus.APPROVED)
                .description("Payment received")
                .build();
        
        paymentTx = transactionRepository.save(paymentTx);
        
        // Update account balance
        account.setCurrentBalance(account.getCurrentBalance().subtract(request.getAmount()));
        accountRepository.save(account);
        
        log.info("Payment processed: {}", paymentTxId);
        
        return toTransactionResponse(paymentTx);
    }
    
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByDateRange(Long accountId, 
                                                                  LocalDateTime startDate, 
                                                                  LocalDateTime endDate) {
        return transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
        return toTransactionResponse(transaction);
    }
    
    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .transactionType(transaction.getTransactionType().name())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .merchantName(transaction.getMerchantName())
                .merchantCategoryCode(transaction.getMerchantCategoryCode())
                .description(transaction.getDescription())
                .referenceId(transaction.getReferenceId())
                .authorizationCode(transaction.getAuthorizationCode())
                .installmentCount(transaction.getInstallmentCount())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
    
    private String generateAuthorizationCode() {
        return String.format("%06d", (int)(Math.random() * 1000000));
    }
}
