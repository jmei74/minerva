package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_txn_account_id", columnList = "account_id"),
    @Index(name = "idx_txn_reference_id", columnList = "reference_id"),
    @Index(name = "idx_txn_created_at", columnList = "created_at")
})
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    private String transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Column(name = "transaction_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3)
    private String currency;
    
    @Column(name = "merchant_name", length = 100)
    private String merchantName;
    
    @Column(name = "merchant_category_code", length = 10)
    private String merchantCategoryCode;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    @Column(name = "reference_id", length = 36)
    private String referenceId;
    
    @Column(name = "authorization_code", length = 10)
    private String authorizationCode;
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "installment_count")
    private Integer installmentCount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_schedule_id")
    private InstallmentSchedule installmentSchedule;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    
    public String getMerchantCategoryCode() { return merchantCategoryCode; }
    public void setMerchantCategoryCode(String merchantCategoryCode) { this.merchantCategoryCode = merchantCategoryCode; }
    
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    
    public InstallmentSchedule getInstallmentSchedule() { return installmentSchedule; }
    public void setInstallmentSchedule(InstallmentSchedule installmentSchedule) { this.installmentSchedule = installmentSchedule; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final Transaction txn = new Transaction();
        
        public Builder transactionId(String transactionId) { txn.transactionId = transactionId; return this; }
        public Builder account(Account account) { txn.account = account; return this; }
        public Builder transactionType(TransactionType transactionType) { txn.transactionType = transactionType; return this; }
        public Builder amount(BigDecimal amount) { txn.amount = amount; return this; }
        public Builder currency(String currency) { txn.currency = currency; return this; }
        public Builder merchantName(String merchantName) { txn.merchantName = merchantName; return this; }
        public Builder merchantCategoryCode(String merchantCategoryCode) { txn.merchantCategoryCode = merchantCategoryCode; return this; }
        public Builder status(TransactionStatus status) { txn.status = status; return this; }
        public Builder referenceId(String referenceId) { txn.referenceId = referenceId; return this; }
        public Builder authorizationCode(String authorizationCode) { txn.authorizationCode = authorizationCode; return this; }
        public Builder description(String description) { txn.description = description; return this; }
        public Builder installmentCount(Integer installmentCount) { txn.installmentCount = installmentCount; return this; }
        public Builder installmentSchedule(InstallmentSchedule installmentSchedule) { txn.installmentSchedule = installmentSchedule; return this; }
        public Builder createdAt(LocalDateTime createdAt) { txn.createdAt = createdAt; return this; }
        public Transaction build() { return txn; }
    }
    
    public enum TransactionType {
        AUTHORIZATION, CAPTURE, REFUND, PAYMENT, REVERSAL
    }
    
    public enum TransactionStatus {
        PENDING, APPROVED, DECLINED, SETTLED, CANCELLED, REVERSED
    }
}
