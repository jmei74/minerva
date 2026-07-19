package com.minerva.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BillResponse {
    
    private String billId;
    private Long accountId;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private LocalDate statementDate;
    private LocalDate paymentDueDate;
    private BigDecimal totalAmount;
    private BigDecimal minimumPayment;
    private String status;
    private List<BillItemResponse> items;
    
    public BillResponse() {}
    
    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }
    
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    
    public LocalDate getBillingPeriodStart() { return billingPeriodStart; }
    public void setBillingPeriodStart(LocalDate billingPeriodStart) { this.billingPeriodStart = billingPeriodStart; }
    
    public LocalDate getBillingPeriodEnd() { return billingPeriodEnd; }
    public void setBillingPeriodEnd(LocalDate billingPeriodEnd) { this.billingPeriodEnd = billingPeriodEnd; }
    
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(LocalDate paymentDueDate) { this.paymentDueDate = paymentDueDate; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BigDecimal getMinimumPayment() { return minimumPayment; }
    public void setMinimumPayment(BigDecimal minimumPayment) { this.minimumPayment = minimumPayment; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<BillItemResponse> getItems() { return items; }
    public void setItems(List<BillItemResponse> items) { this.items = items; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final BillResponse resp = new BillResponse();
        
        public Builder billId(String billId) { resp.billId = billId; return this; }
        public Builder accountId(Long accountId) { resp.accountId = accountId; return this; }
        public Builder billingPeriodStart(LocalDate billingPeriodStart) { resp.billingPeriodStart = billingPeriodStart; return this; }
        public Builder billingPeriodEnd(LocalDate billingPeriodEnd) { resp.billingPeriodEnd = billingPeriodEnd; return this; }
        public Builder statementDate(LocalDate statementDate) { resp.statementDate = statementDate; return this; }
        public Builder paymentDueDate(LocalDate paymentDueDate) { resp.paymentDueDate = paymentDueDate; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { resp.totalAmount = totalAmount; return this; }
        public Builder minimumPayment(BigDecimal minimumPayment) { resp.minimumPayment = minimumPayment; return this; }
        public Builder status(String status) { resp.status = status; return this; }
        public Builder items(List<BillItemResponse> items) { resp.items = items; return this; }
        public BillResponse build() { return resp; }
    }
}
