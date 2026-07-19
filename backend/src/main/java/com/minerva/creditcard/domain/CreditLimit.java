package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_limits")
public class CreditLimit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;
    
    @Column(name = "total_credit_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCreditLimit;
    
    @Column(name = "available_credit_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal availableCreditLimit;
    
    @Column(name = "used_credit_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal usedCreditLimit;
    
    @Column(name = "temporary_limit", precision = 15, scale = 2)
    private BigDecimal temporaryLimit;
    
    @Column(name = "temporary_limit_expiry")
    private LocalDate temporaryLimitExpiry;
    
    @Column(name = "cash_advance_limit", precision = 15, scale = 2)
    private BigDecimal cashAdvanceLimit;
    
    @Column(name = "cash_advance_used", precision = 15, scale = 2)
    private BigDecimal cashAdvanceUsed;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    public BigDecimal getTotalCreditLimit() { return totalCreditLimit; }
    public void setTotalCreditLimit(BigDecimal totalCreditLimit) { this.totalCreditLimit = totalCreditLimit; }
    
    public BigDecimal getAvailableCreditLimit() { return availableCreditLimit; }
    public void setAvailableCreditLimit(BigDecimal availableCreditLimit) { this.availableCreditLimit = availableCreditLimit; }
    
    public BigDecimal getUsedCreditLimit() { return usedCreditLimit; }
    public void setUsedCreditLimit(BigDecimal usedCreditLimit) { this.usedCreditLimit = usedCreditLimit; }
    
    public BigDecimal getTemporaryLimit() { return temporaryLimit; }
    public void setTemporaryLimit(BigDecimal temporaryLimit) { this.temporaryLimit = temporaryLimit; }
    
    public LocalDate getTemporaryLimitExpiry() { return temporaryLimitExpiry; }
    public void setTemporaryLimitExpiry(LocalDate temporaryLimitExpiry) { this.temporaryLimitExpiry = temporaryLimitExpiry; }
    
    public BigDecimal getCashAdvanceLimit() { return cashAdvanceLimit; }
    public void setCashAdvanceLimit(BigDecimal cashAdvanceLimit) { this.cashAdvanceLimit = cashAdvanceLimit; }
    
    public BigDecimal getCashAdvanceUsed() { return cashAdvanceUsed; }
    public void setCashAdvanceUsed(BigDecimal cashAdvanceUsed) { this.cashAdvanceUsed = cashAdvanceUsed; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean hasAvailableCredit(BigDecimal amount) {
        BigDecimal effectiveAvailable = availableCreditLimit;
        if (temporaryLimit != null && temporaryLimitExpiry != null 
                && !temporaryLimitExpiry.isBefore(LocalDate.now())) {
            effectiveAvailable = effectiveAvailable.add(temporaryLimit);
        }
        return effectiveAvailable.compareTo(amount) >= 0;
    }
    
    public void useCredit(BigDecimal amount) {
        if (availableCreditLimit.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient credit limit");
        }
        availableCreditLimit = availableCreditLimit.subtract(amount);
        usedCreditLimit = usedCreditLimit.add(amount);
    }
    
    public void releaseCredit(BigDecimal amount) {
        availableCreditLimit = availableCreditLimit.add(amount);
        if (usedCreditLimit.compareTo(amount) >= 0) {
            usedCreditLimit = usedCreditLimit.subtract(amount);
        } else {
            usedCreditLimit = BigDecimal.ZERO;
        }
    }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final CreditLimit cl = new CreditLimit();
        
        public Builder account(Account account) { cl.account = account; return this; }
        public Builder totalCreditLimit(BigDecimal totalCreditLimit) { cl.totalCreditLimit = totalCreditLimit; return this; }
        public Builder availableCreditLimit(BigDecimal availableCreditLimit) { cl.availableCreditLimit = availableCreditLimit; return this; }
        public Builder usedCreditLimit(BigDecimal usedCreditLimit) { cl.usedCreditLimit = usedCreditLimit; return this; }
        public Builder temporaryLimit(BigDecimal temporaryLimit) { cl.temporaryLimit = temporaryLimit; return this; }
        public Builder temporaryLimitExpiry(LocalDate temporaryLimitExpiry) { cl.temporaryLimitExpiry = temporaryLimitExpiry; return this; }
        public Builder cashAdvanceLimit(BigDecimal cashAdvanceLimit) { cl.cashAdvanceLimit = cashAdvanceLimit; return this; }
        public Builder cashAdvanceUsed(BigDecimal cashAdvanceUsed) { cl.cashAdvanceUsed = cashAdvanceUsed; return this; }
        public CreditLimit build() { return cl; }
    }
}
