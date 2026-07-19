package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "installment_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    
    public enum InstallmentStatus {
        ACTIVE, COMPLETED, CANCELLED, DEFAULTED
    }
}
