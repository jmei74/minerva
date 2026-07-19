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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentService {
    
    private final InstallmentScheduleRepository installmentScheduleRepository;
    private final TransactionRepository transactionRepository;
    private final CreditLimitRepository creditLimitRepository;
    private final AccountRepository accountRepository;
    
    @Transactional
    public InstallmentResponse createInstallment(InstallmentRequest request) {
        log.info("Creating installment for transaction: {}", request.getTransactionId());
        
        Transaction transaction = transactionRepository.findByTransactionIdForUpdate(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", request.getTransactionId()));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.APPROVED) {
            throw new BusinessException("Transaction must be approved for installment");
        }
        
        if (transaction.getInstallmentSchedule() != null) {
            throw new BusinessException("Transaction already has installment");
        }
        
        BigDecimal principal = transaction.getAmount();
        BigDecimal apr = request.getApr();
        int months = request.getInstallmentCount();
        
        // Calculate total interest using simple interest formula
        BigDecimal totalInterest = principal.multiply(apr)
                .multiply(BigDecimal.valueOf(months))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalAmount = principal.add(totalInterest);
        BigDecimal monthlyPayment = totalAmount.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        
        // Adjust for rounding difference in last payment
        BigDecimal totalOfPayments = monthlyPayment.multiply(BigDecimal.valueOf(months));
        BigDecimal roundingDiff = totalAmount.subtract(totalOfPayments);
        monthlyPayment = monthlyPayment.add(roundingDiff.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP));
        
        String scheduleId = UUID.randomUUID().toString();
        
        InstallmentSchedule schedule = InstallmentSchedule.builder()
                .scheduleId(scheduleId)
                .account(transaction.getAccount())
                .originalTransaction(transaction)
                .principalAmount(principal)
                .totalInterest(totalInterest)
                .totalAmount(totalAmount)
                .monthlyPayment(monthlyPayment)
                .installmentCount(months)
                .installmentsRemaining(months)
                .remainingPrincipal(principal)
                .apr(apr)
                .status(InstallmentSchedule.InstallmentStatus.ACTIVE)
                .startDate(LocalDate.now())
                .nextPaymentDate(LocalDate.now().plusMonths(1))
                .build();
        
        schedule = installmentScheduleRepository.save(schedule);
        
        // Update transaction with installment reference
        transaction.setInstallmentSchedule(schedule);
        transactionRepository.save(transaction);
        
        // Reserve credit for remaining installments
        CreditLimit creditLimit = creditLimitRepository.findByAccountIdForUpdate(transaction.getAccount().getId())
                .orElseThrow(() -> new BusinessException("Credit limit not found"));
        
        creditLimit.useCredit(totalAmount.subtract(principal));
        creditLimitRepository.save(creditLimit);
        
        log.info("Installment schedule created: {}", scheduleId);
        
        return toInstallmentResponse(schedule);
    }
    
    @Transactional(readOnly = true)
    public InstallmentResponse getInstallment(String scheduleId) {
        InstallmentSchedule schedule = installmentScheduleRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("InstallmentSchedule", scheduleId));
        return toInstallmentResponse(schedule);
    }
    
    @Transactional(readOnly = true)
    public java.util.List<InstallmentResponse> getInstallmentsByAccount(Long accountId) {
        return installmentScheduleRepository.findByAccountId(accountId)
                .stream()
                .map(this::toInstallmentResponse)
                .toList();
    }
    
    @Transactional
    public InstallmentResponse processPartialRefund(String scheduleId, BigDecimal refundAmount) {
        log.info("Processing partial refund for installment: {}, amount: {}", scheduleId, refundAmount);
        
        InstallmentSchedule schedule = installmentScheduleRepository.findById(
                installmentScheduleRepository.findByScheduleId(scheduleId).map(InstallmentSchedule::getId).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("InstallmentSchedule", scheduleId));
        
        if (schedule.getStatus() != InstallmentSchedule.InstallmentStatus.ACTIVE) {
            throw new BusinessException("Installment schedule is not active");
        }
        
        if (refundAmount.compareTo(schedule.getRemainingPrincipal()) > 0) {
            throw new BusinessException("Refund amount exceeds remaining principal");
        }
        
        // Calculate new remaining principal and monthly payment
        BigDecimal newRemainingPrincipal = schedule.getRemainingPrincipal().subtract(refundAmount);
        BigDecimal monthlyPaymentNew = newRemainingPrincipal.divide(
                BigDecimal.valueOf(schedule.getInstallmentsRemaining()), 2, RoundingMode.HALF_UP);
        
        // Release credit for refunded amount
        CreditLimit creditLimit = creditLimitRepository.findByAccountIdForUpdate(schedule.getAccount().getId())
                .orElseThrow(() -> new BusinessException("Credit limit not found"));
        
        creditLimit.releaseCredit(refundAmount);
        creditLimitRepository.save(creditLimit);
        
        // Update installment schedule
        schedule.setRemainingPrincipal(newRemainingPrincipal);
        schedule.setMonthlyPayment(monthlyPaymentNew);
        schedule = installmentScheduleRepository.save(schedule);
        
        log.info("Partial refund processed for installment: {}", scheduleId);
        
        return toInstallmentResponse(schedule);
    }
    
    @Transactional
    public InstallmentResponse earlySettle(String scheduleId) {
        log.info("Processing early settlement for installment: {}", scheduleId);
        
        InstallmentSchedule schedule = installmentScheduleRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("InstallmentSchedule", scheduleId));
        
        if (schedule.getStatus() != InstallmentSchedule.InstallmentStatus.ACTIVE) {
            throw new BusinessException("Installment schedule is not active");
        }
        
        // Release remaining credit
        CreditLimit creditLimit = creditLimitRepository.findByAccountIdForUpdate(schedule.getAccount().getId())
                .orElseThrow(() -> new BusinessException("Credit limit not found"));
        
        creditLimit.releaseCredit(schedule.getRemainingPrincipal());
        creditLimitRepository.save(creditLimit);
        
        // Mark as completed
        schedule.setStatus(InstallmentSchedule.InstallmentStatus.COMPLETED);
        schedule.setInstallmentsRemaining(0);
        schedule = installmentScheduleRepository.save(schedule);
        
        log.info("Early settlement completed for installment: {}", scheduleId);
        
        return toInstallmentResponse(schedule);
    }
    
    private InstallmentResponse toInstallmentResponse(InstallmentSchedule schedule) {
        return InstallmentResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .status(schedule.getStatus().name())
                .principalAmount(schedule.getPrincipalAmount())
                .totalInterest(schedule.getTotalInterest())
                .totalAmount(schedule.getTotalAmount())
                .monthlyPayment(schedule.getMonthlyPayment())
                .installmentCount(schedule.getInstallmentCount())
                .installmentsRemaining(schedule.getInstallmentsRemaining())
                .remainingPrincipal(schedule.getRemainingPrincipal())
                .startDate(schedule.getStartDate())
                .nextPaymentDate(schedule.getNextPaymentDate())
                .build();
    }
}
