package com.minerva.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    
    private String transactionId;
    private String transactionType;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String merchantCategoryCode;
    private String description;
    private String referenceId;
    private String authorizationCode;
    private Integer installmentCount;
    private LocalDateTime createdAt;
    
    public TransactionResponse() {}
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    
    public String getMerchantCategoryCode() { return merchantCategoryCode; }
    public void setMerchantCategoryCode(String merchantCategoryCode) { this.merchantCategoryCode = merchantCategoryCode; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final TransactionResponse resp = new TransactionResponse();
        
        public Builder transactionId(String transactionId) { resp.transactionId = transactionId; return this; }
        public Builder transactionType(String transactionType) { resp.transactionType = transactionType; return this; }
        public Builder status(String status) { resp.status = status; return this; }
        public Builder amount(BigDecimal amount) { resp.amount = amount; return this; }
        public Builder currency(String currency) { resp.currency = currency; return this; }
        public Builder merchantName(String merchantName) { resp.merchantName = merchantName; return this; }
        public Builder merchantCategoryCode(String merchantCategoryCode) { resp.merchantCategoryCode = merchantCategoryCode; return this; }
        public Builder description(String description) { resp.description = description; return this; }
        public Builder referenceId(String referenceId) { resp.referenceId = referenceId; return this; }
        public Builder authorizationCode(String authorizationCode) { resp.authorizationCode = authorizationCode; return this; }
        public Builder installmentCount(Integer installmentCount) { resp.installmentCount = installmentCount; return this; }
        public Builder createdAt(LocalDateTime createdAt) { resp.createdAt = createdAt; return this; }
        public TransactionResponse build() { return resp; }
    }
}
