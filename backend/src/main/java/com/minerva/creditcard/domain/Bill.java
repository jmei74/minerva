package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "bill_id", unique = true, nullable = false, length = 36)
    private String billId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;
    
    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;
    
    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;
    
    @Column(name = "payment_due_date", nullable = false)
    private LocalDate paymentDueDate;
    
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Column(name = "minimum_payment", precision = 15, scale = 2, nullable = false)
    private BigDecimal minimumPayment;
    
    @Column(name = "previous_balance", precision = 15, scale = 2)
    private BigDecimal previousBalance;
    
    @Column(name = "new_charges", precision = 15, scale = 2)
    private BigDecimal newCharges;
    
    @Column(name = "payments_refunds", precision = 15, scale = 2)
    private BigDecimal paymentsRefunds;
    
    @Column(name = "adjustments", precision = 15, scale = 2)
    private BigDecimal adjustments;
    
    @Column(name = "interest_charges", precision = 15, scale = 2)
    private BigDecimal interestCharges;
    
    @Column(name = "apr", precision = 6, scale = 4)
    private BigDecimal apr;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BillStatus status;
    
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BillItem> items = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = BillStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum BillStatus {
        PENDING, ISSUED, OVERDUE, PARTIALLY_PAID, PAID, WRITTEN_OFF
    }
}
