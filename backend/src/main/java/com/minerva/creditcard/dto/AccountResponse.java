package com.minerva.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {
    
    private Long id;
    private String accountNo;
    private Long customerId;
    private String accountType;
    private String status;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal currentBalance;
    private Integer billingCycleDay;
    private String token;
    private LocalDateTime createdAt;
    
    public AccountResponse() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    
    public BigDecimal getAvailableCredit() { return availableCredit; }
    public void setAvailableCredit(BigDecimal availableCredit) { this.availableCredit = availableCredit; }
    
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    
    public Integer getBillingCycleDay() { return billingCycleDay; }
    public void setBillingCycleDay(Integer billingCycleDay) { this.billingCycleDay = billingCycleDay; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final AccountResponse resp = new AccountResponse();
        
        public Builder id(Long id) { resp.id = id; return this; }
        public Builder accountNo(String accountNo) { resp.accountNo = accountNo; return this; }
        public Builder customerId(Long customerId) { resp.customerId = customerId; return this; }
        public Builder accountType(String accountType) { resp.accountType = accountType; return this; }
        public Builder status(String status) { resp.status = status; return this; }
        public Builder creditLimit(BigDecimal creditLimit) { resp.creditLimit = creditLimit; return this; }
        public Builder availableCredit(BigDecimal availableCredit) { resp.availableCredit = availableCredit; return this; }
        public Builder currentBalance(BigDecimal currentBalance) { resp.currentBalance = currentBalance; return this; }
        public Builder billingCycleDay(Integer billingCycleDay) { resp.billingCycleDay = billingCycleDay; return this; }
        public Builder token(String token) { resp.token = token; return this; }
        public Builder createdAt(LocalDateTime createdAt) { resp.createdAt = createdAt; return this; }
        public AccountResponse build() { return resp; }
    }
}
