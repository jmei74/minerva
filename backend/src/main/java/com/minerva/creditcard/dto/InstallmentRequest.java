package com.minerva.creditcard.dto;

import java.math.BigDecimal;

public class InstallmentRequest {
    
    private String transactionId;
    private Integer installmentCount;
    private BigDecimal apr;
    
    public InstallmentRequest() {}
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    
    public BigDecimal getApr() { return apr; }
    public void setApr(BigDecimal apr) { this.apr = apr; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final InstallmentRequest req = new InstallmentRequest();
        
        public Builder transactionId(String transactionId) { req.transactionId = transactionId; return this; }
        public Builder installmentCount(Integer installmentCount) { req.installmentCount = installmentCount; return this; }
        public Builder apr(BigDecimal apr) { req.apr = apr; return this; }
        public InstallmentRequest build() { return req; }
    }
}
