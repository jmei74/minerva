package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_limits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    
    public boolean hasAvailableCredit(BigDecimal amount) {
        BigDecimal effectiveLimit = totalCreditLimit;
        if (temporaryLimit != null && temporaryLimitExpiry != null 
                && !temporaryLimitExpiry.isBefore(LocalDate.now())) {
            effectiveLimit = effectiveLimit.add(temporaryLimit);
        }
        return availableCreditLimit.compareTo(amount) >= 0;
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
        usedCreditLimit = usedCreditLimit.subtract(amount);
    }
}
