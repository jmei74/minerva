package com.minerva.creditcard.dto;

import java.math.BigDecimal;

public class RefundRequest {
    
    private String originalTransactionId;
    private BigDecimal amount;
    private String reason;
    
    public RefundRequest() {}
    
    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final RefundRequest req = new RefundRequest();
        
        public Builder originalTransactionId(String originalTransactionId) { req.originalTransactionId = originalTransactionId; return this; }
        public Builder amount(BigDecimal amount) { req.amount = amount; return this; }
        public Builder reason(String reason) { req.reason = reason; return this; }
        public RefundRequest build() { return req; }
    }
}
