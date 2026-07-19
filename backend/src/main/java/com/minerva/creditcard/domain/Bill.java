package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
public class Bill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "bill_id", unique = true, nullable = false, length = 36)
    private String billId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;
    
    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;
    
    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;
    
    @Column(name = "payment_due_date", nullable = false)
    private LocalDate paymentDueDate;
    
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Column(name = "minimum_payment", precision = 15, scale = 2, nullable = false)
    private BigDecimal minimumPayment;
    
    @Column(name = "previous_balance", precision = 15, scale = 2)
    private BigDecimal previousBalance;
    
    @Column(name = "new_charges", precision = 15, scale = 2)
    private BigDecimal newCharges;
    
    @Column(name = "payments_refunds", precision = 15, scale = 2)
    private BigDecimal paymentsRefunds;
    
    @Column(name = "adjustments", precision = 15, scale = 2)
    private BigDecimal adjustments;
    
    @Column(name = "interest_charges", precision = 15, scale = 2)
    private BigDecimal interestCharges;
    
    @Column(name = "apr", precision = 6, scale = 4)
    private BigDecimal apr;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BillStatus status;
    
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BillItem> items = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = BillStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
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
    
    public BigDecimal getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(BigDecimal previousBalance) { this.previousBalance = previousBalance; }
    
    public BigDecimal getNewCharges() { return newCharges; }
    public void setNewCharges(BigDecimal newCharges) { this.newCharges = newCharges; }
    
    public BigDecimal getPaymentsRefunds() { return paymentsRefunds; }
    public void setPaymentsRefunds(BigDecimal paymentsRefunds) { this.paymentsRefunds = paymentsRefunds; }
    
    public BigDecimal getAdjustments() { return adjustments; }
    public void setAdjustments(BigDecimal adjustments) { this.adjustments = adjustments; }
    
    public BigDecimal getInterestCharges() { return interestCharges; }
    public void setInterestCharges(BigDecimal interestCharges) { this.interestCharges = interestCharges; }
    
    public BigDecimal getApr() { return apr; }
    public void setApr(BigDecimal apr) { this.apr = apr; }
    
    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }
    
    public List<BillItem> getItems() { return items; }
    public void setItems(List<BillItem> items) { this.items = items; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final Bill bill = new Bill();
        
        public Builder billId(String billId) { bill.billId = billId; return this; }
        public Builder account(Account account) { bill.account = account; return this; }
        public Builder billingPeriodStart(LocalDate billingPeriodStart) { bill.billingPeriodStart = billingPeriodStart; return this; }
        public Builder billingPeriodEnd(LocalDate billingPeriodEnd) { bill.billingPeriodEnd = billingPeriodEnd; return this; }
        public Builder statementDate(LocalDate statementDate) { bill.statementDate = statementDate; return this; }
        public Builder paymentDueDate(LocalDate paymentDueDate) { bill.paymentDueDate = paymentDueDate; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { bill.totalAmount = totalAmount; return this; }
        public Builder minimumPayment(BigDecimal minimumPayment) { bill.minimumPayment = minimumPayment; return this; }
        public Builder previousBalance(BigDecimal previousBalance) { bill.previousBalance = previousBalance; return this; }
        public Builder newCharges(BigDecimal newCharges) { bill.newCharges = newCharges; return this; }
        public Builder paymentsRefunds(BigDecimal paymentsRefunds) { bill.paymentsRefunds = paymentsRefunds; return this; }
        public Builder adjustments(BigDecimal adjustments) { bill.adjustments = adjustments; return this; }
        public Builder interestCharges(BigDecimal interestCharges) { bill.interestCharges = interestCharges; return this; }
        public Builder apr(BigDecimal apr) { bill.apr = apr; return this; }
        public Builder status(BillStatus status) { bill.status = status; return this; }
        public Builder items(List<BillItem> items) { bill.items = items; return this; }
        public Bill build() { return bill; }
    }
    
    public enum BillStatus {
        PENDING, ISSUED, OVERDUE, PARTIALLY_PAID, PAID, WRITTEN_OFF
    }
}
