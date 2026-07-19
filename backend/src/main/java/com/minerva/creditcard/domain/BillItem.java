package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "item_type", length = 20)
    @Enumerated(EnumType.STRING)
    private BillItemType itemType;
    
    @Column(name = "is_installment")
    private Boolean isInstallment;
    
    @Column(name = "installment_number")
    private Integer installmentNumber;
    
    @Column(name = "total_installments")
    private Integer totalInstallments;
    
    public enum BillItemType {
        PURCHASE, REFUND, PAYMENT, INTEREST, FEE, INSTALLMENT
    }
}
