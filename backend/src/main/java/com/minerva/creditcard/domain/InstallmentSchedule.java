package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "installment_schedules")
public class InstallmentSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "schedule_id", unique = true, nullable = false, length = 36)
    private String scheduleId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction originalTransaction;
    
    @Column(name = "principal_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal principalAmount;
    
    @Column(name = "total_interest", precision = 15, scale = 2)
    private BigDecimal totalInterest;
    
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Column(name = "monthly_payment", precision = 15, scale = 2, nullable = false)
    private BigDecimal monthlyPayment;
    
    @Column(name = "installment_count", nullable = false)
    private Integer installmentCount;
    
    @Column(name = "installments_remaining")
    private Integer installmentsRemaining;
    
    @Column(name = "remaining_principal", precision = 15, scale = 2)
    private BigDecimal remainingPrincipal;
    
    @Column(name = "apr", precision = 6, scale = 4)
    private BigDecimal apr;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InstallmentStatus status;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = InstallmentStatus.ACTIVE;
        }
        if (installmentsRemaining == null) {
            installmentsRemaining = installmentCount;
        }
        if (remainingPrincipal == null) {
            remainingPrincipal = principalAmount;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    public Transaction getOriginalTransaction() { return originalTransaction; }
    public void setOriginalTransaction(Transaction originalTransaction) { this.originalTransaction = originalTransaction; }
    
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    
    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    
    public Integer getInstallmentsRemaining() { return installmentsRemaining; }
    public void setInstallmentsRemaining(Integer installmentsRemaining) { this.installmentsRemaining = installmentsRemaining; }
    
    public BigDecimal getRemainingPrincipal() { return remainingPrincipal; }
    public void setRemainingPrincipal(BigDecimal remainingPrincipal) { this.remainingPrincipal = remainingPrincipal; }
    
    public BigDecimal getApr() { return apr; }
    public void setApr(BigDecimal apr) { this.apr = apr; }
    
    public InstallmentStatus getStatus() { return status; }
    public void setStatus(InstallmentStatus status) { this.status = status; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(LocalDate nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final InstallmentSchedule schedule = new InstallmentSchedule();
        
        public Builder scheduleId(String scheduleId) { schedule.scheduleId = scheduleId; return this; }
        public Builder account(Account account) { schedule.account = account; return this; }
        public Builder originalTransaction(Transaction originalTransaction) { schedule.originalTransaction = originalTransaction; return this; }
        public Builder principalAmount(BigDecimal principalAmount) { schedule.principalAmount = principalAmount; return this; }
        public Builder totalInterest(BigDecimal totalInterest) { schedule.totalInterest = totalInterest; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { schedule.totalAmount = totalAmount; return this; }
        public Builder monthlyPayment(BigDecimal monthlyPayment) { schedule.monthlyPayment = monthlyPayment; return this; }
        public Builder installmentCount(Integer installmentCount) { schedule.installmentCount = installmentCount; return this; }
        public Builder installmentsRemaining(Integer installmentsRemaining) { schedule.installmentsRemaining = installmentsRemaining; return this; }
        public Builder remainingPrincipal(BigDecimal remainingPrincipal) { schedule.remainingPrincipal = remainingPrincipal; return this; }
        public Builder apr(BigDecimal apr) { schedule.apr = apr; return this; }
        public Builder status(InstallmentStatus status) { schedule.status = status; return this; }
        public Builder startDate(LocalDate startDate) { schedule.startDate = startDate; return this; }
        public Builder nextPaymentDate(LocalDate nextPaymentDate) { schedule.nextPaymentDate = nextPaymentDate; return this; }
        public InstallmentSchedule build() { return schedule; }
    }
    
    public enum InstallmentStatus {
        ACTIVE, COMPLETED, CANCELLED, DEFAULTED
    }
}
