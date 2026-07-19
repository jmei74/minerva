package com.minerva.creditcard.dto;

import java.math.BigDecimal;

public class AuthorizationRequest {
    
    private String token;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String merchantCategoryCode;
    private Integer installmentCount;
    private String description;
    
    public AuthorizationRequest() {}
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    
    public String getMerchantCategoryCode() { return merchantCategoryCode; }
    public void setMerchantCategoryCode(String merchantCategoryCode) { this.merchantCategoryCode = merchantCategoryCode; }
    
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final AuthorizationRequest req = new AuthorizationRequest();
        
        public Builder token(String token) { req.token = token; return this; }
        public Builder amount(BigDecimal amount) { req.amount = amount; return this; }
        public Builder currency(String currency) { req.currency = currency; return this; }
        public Builder merchantName(String merchantName) { req.merchantName = merchantName; return this; }
        public Builder merchantCategoryCode(String merchantCategoryCode) { req.merchantCategoryCode = merchantCategoryCode; return this; }
        public Builder installmentCount(Integer installmentCount) { req.installmentCount = installmentCount; return this; }
        public Builder description(String description) { req.description = description; return this; }
        public AuthorizationRequest build() { return req; }
    }
}
