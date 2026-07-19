package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_items")
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
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Bill getBill() { return bill; }
    public void setBill(Bill bill) { this.bill = bill; }
    
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public BillItemType getItemType() { return itemType; }
    public void setItemType(BillItemType itemType) { this.itemType = itemType; }
    
    public Boolean getIsInstallment() { return isInstallment; }
    public void setIsInstallment(Boolean isInstallment) { this.isInstallment = isInstallment; }
    
    public Integer getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }
    
    public Integer getTotalInstallments() { return totalInstallments; }
    public void setTotalInstallments(Integer totalInstallments) { this.totalInstallments = totalInstallments; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final BillItem item = new BillItem();
        
        public Builder bill(Bill bill) { item.bill = bill; return this; }
        public Builder transaction(Transaction transaction) { item.transaction = transaction; return this; }
        public Builder description(String description) { item.description = description; return this; }
        public Builder transactionDate(LocalDateTime transactionDate) { item.transactionDate = transactionDate; return this; }
        public Builder amount(BigDecimal amount) { item.amount = amount; return this; }
        public Builder itemType(BillItemType itemType) { item.itemType = itemType; return this; }
        public Builder isInstallment(Boolean isInstallment) { item.isInstallment = isInstallment; return this; }
        public Builder installmentNumber(Integer installmentNumber) { item.installmentNumber = installmentNumber; return this; }
        public Builder totalInstallments(Integer totalInstallments) { item.totalInstallments = totalInstallments; return this; }
        public BillItem build() { return item; }
    }
    
    public enum BillItemType {
        PURCHASE, REFUND, PAYMENT, INTEREST, FEE, INSTALLMENT
    }
}
