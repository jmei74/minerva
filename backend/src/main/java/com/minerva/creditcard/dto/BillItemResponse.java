package com.minerva.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BillItemResponse {
    
    private Long id;
    private String description;
    private LocalDateTime transactionDate;
    private BigDecimal amount;
    private String itemType;
    private Boolean isInstallment;
    private Integer installmentNumber;
    private Integer totalInstallments;
    
    public BillItemResponse() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    
    public Boolean getIsInstallment() { return isInstallment; }
    public void setIsInstallment(Boolean isInstallment) { this.isInstallment = isInstallment; }
    
    public Integer getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }
    
    public Integer getTotalInstallments() { return totalInstallments; }
    public void setTotalInstallments(Integer totalInstallments) { this.totalInstallments = totalInstallments; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final BillItemResponse resp = new BillItemResponse();
        
        public Builder id(Long id) { resp.id = id; return this; }
        public Builder description(String description) { resp.description = description; return this; }
        public Builder transactionDate(LocalDateTime transactionDate) { resp.transactionDate = transactionDate; return this; }
        public Builder amount(BigDecimal amount) { resp.amount = amount; return this; }
        public Builder itemType(String itemType) { resp.itemType = itemType; return this; }
        public Builder isInstallment(Boolean isInstallment) { resp.isInstallment = isInstallment; return this; }
        public Builder installmentNumber(Integer installmentNumber) { resp.installmentNumber = installmentNumber; return this; }
        public Builder totalInstallments(Integer totalInstallments) { resp.totalInstallments = totalInstallments; return this; }
        public BillItemResponse build() { return resp; }
    }
}
