package com.minerva.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InstallmentResponse {
    
    private String scheduleId;
    private String status;
    private BigDecimal principalAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalAmount;
    private BigDecimal monthlyPayment;
    private Integer installmentCount;
    private Integer installmentsRemaining;
    private BigDecimal remainingPrincipal;
    private LocalDate startDate;
    private LocalDate nextPaymentDate;
    
    public InstallmentResponse() {}
    
    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    
    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    
    public Integer getInstallmentsRemaining() { return installmentsRemaining; }
    public void setInstallmentsRemaining(Integer installmentsRemaining) { this.installmentsRemaining = installmentsRemaining; }
    
    public BigDecimal getRemainingPrincipal() { return remainingPrincipal; }
    public void setRemainingPrincipal(BigDecimal remainingPrincipal) { this.remainingPrincipal = remainingPrincipal; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(LocalDate nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final InstallmentResponse resp = new InstallmentResponse();
        
        public Builder scheduleId(String scheduleId) { resp.scheduleId = scheduleId; return this; }
        public Builder status(String status) { resp.status = status; return this; }
        public Builder principalAmount(BigDecimal principalAmount) { resp.principalAmount = principalAmount; return this; }
        public Builder totalInterest(BigDecimal totalInterest) { resp.totalInterest = totalInterest; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { resp.totalAmount = totalAmount; return this; }
        public Builder monthlyPayment(BigDecimal monthlyPayment) { resp.monthlyPayment = monthlyPayment; return this; }
        public Builder installmentCount(Integer installmentCount) { resp.installmentCount = installmentCount; return this; }
        public Builder installmentsRemaining(Integer installmentsRemaining) { resp.installmentsRemaining = installmentsRemaining; return this; }
        public Builder remainingPrincipal(BigDecimal remainingPrincipal) { resp.remainingPrincipal = remainingPrincipal; return this; }
        public Builder startDate(LocalDate startDate) { resp.startDate = startDate; return this; }
        public Builder nextPaymentDate(LocalDate nextPaymentDate) { resp.nextPaymentDate = nextPaymentDate; return this; }
        public InstallmentResponse build() { return resp; }
    }
}
