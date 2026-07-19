package com.minerva.creditcard.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    
    private String accountNo;
    private BigDecimal amount;
    private String currency;
    
    public PaymentRequest() {}
    
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final PaymentRequest req = new PaymentRequest();
        
        public Builder accountNo(String accountNo) { req.accountNo = accountNo; return this; }
        public Builder amount(BigDecimal amount) { req.amount = amount; return this; }
        public Builder currency(String currency) { req.currency = currency; return this; }
        public PaymentRequest build() { return req; }
    }
}
