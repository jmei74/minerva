package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_no", unique = true, nullable = false, length = 20)
    private String accountNo;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "account_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;
    
    @Column(name = "available_credit", precision = 15, scale = 2)
    private BigDecimal availableCredit;
    
    @Column(name = "current_balance", precision = 15, scale = 2)
    private BigDecimal currentBalance;
    
    @Column(name = "billing_cycle_day")
    private Integer billingCycleDay;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    
    public BigDecimal getAvailableCredit() { return availableCredit; }
    public void setAvailableCredit(BigDecimal availableCredit) { this.availableCredit = availableCredit; }
    
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    
    public Integer getBillingCycleDay() { return billingCycleDay; }
    public void setBillingCycleDay(Integer billingCycleDay) { this.billingCycleDay = billingCycleDay; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final Account account = new Account();
        
        public Builder accountNo(String accountNo) { account.accountNo = accountNo; return this; }
        public Builder customerId(Long customerId) { account.customerId = customerId; return this; }
        public Builder accountType(AccountType accountType) { account.accountType = accountType; return this; }
        public Builder status(AccountStatus status) { account.status = status; return this; }
        public Builder creditLimit(BigDecimal creditLimit) { account.creditLimit = creditLimit; return this; }
        public Builder availableCredit(BigDecimal availableCredit) { account.availableCredit = availableCredit; return this; }
        public Builder currentBalance(BigDecimal currentBalance) { account.currentBalance = currentBalance; return this; }
        public Builder billingCycleDay(Integer billingCycleDay) { account.billingCycleDay = billingCycleDay; return this; }
        public Account build() { return account; }
    }
    
    public enum AccountType {
        CREDIT_CARD, INSTALLMENT
    }
    
    public enum AccountStatus {
        PENDING, ACTIVE, SUSPENDED, CLOSED
    }
}
