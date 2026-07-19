package com.minerva.creditcard.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateAccountRequest {
    
    private Long customerId;
    private String accountType;
    private BigDecimal creditLimit;
    private Integer billingCycleDay;
    
    public CreateAccountRequest() {}
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    
    public Integer getBillingCycleDay() { return billingCycleDay; }
    public void setBillingCycleDay(Integer billingCycleDay) { this.billingCycleDay = billingCycleDay; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final CreateAccountRequest req = new CreateAccountRequest();
        
        public Builder customerId(Long customerId) { req.customerId = customerId; return this; }
        public Builder accountType(String accountType) { req.accountType = accountType; return this; }
        public Builder creditLimit(BigDecimal creditLimit) { req.creditLimit = creditLimit; return this; }
        public Builder billingCycleDay(Integer billingCycleDay) { req.billingCycleDay = billingCycleDay; return this; }
        public CreateAccountRequest build() { return req; }
    }
}
