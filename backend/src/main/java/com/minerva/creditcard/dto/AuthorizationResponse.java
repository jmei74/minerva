package com.minerva.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuthorizationResponse {
    
    private String transactionId;
    private String status;
    private String authorizationCode;
    private BigDecimal authorizedAmount;
    private BigDecimal availableCredit;
    private String message;
    private LocalDateTime timestamp;
    
    public AuthorizationResponse() {}
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    
    public BigDecimal getAuthorizedAmount() { return authorizedAmount; }
    public void setAuthorizedAmount(BigDecimal authorizedAmount) { this.authorizedAmount = authorizedAmount; }
    
    public BigDecimal getAvailableCredit() { return availableCredit; }
    public void setAvailableCredit(BigDecimal availableCredit) { this.availableCredit = availableCredit; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final AuthorizationResponse resp = new AuthorizationResponse();
        
        public Builder transactionId(String transactionId) { resp.transactionId = transactionId; return this; }
        public Builder status(String status) { resp.status = status; return this; }
        public Builder authorizationCode(String authorizationCode) { resp.authorizationCode = authorizationCode; return this; }
        public Builder authorizedAmount(BigDecimal authorizedAmount) { resp.authorizedAmount = authorizedAmount; return this; }
        public Builder availableCredit(BigDecimal availableCredit) { resp.availableCredit = availableCredit; return this; }
        public Builder message(String message) { resp.message = message; return this; }
        public Builder timestamp(LocalDateTime timestamp) { resp.timestamp = timestamp; return this; }
        public AuthorizationResponse build() { return resp; }
    }
}
