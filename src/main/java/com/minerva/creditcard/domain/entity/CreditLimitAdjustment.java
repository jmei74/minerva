package com.minerva.creditcard.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_limit_adjustment",
       indexes = {
           @Index(name = "idx_adj_account", columnList = "account_id, created_at DESC")
       })
public class CreditLimitAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "adj_id", updatable = false, nullable = false)
    private UUID adjId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "adj_type", length = 20, nullable = false)
    private String adjType;

    @Column(name = "old_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal oldLimit;

    @Column(name = "new_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal newLimit;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "approval_id", length = 50)
    private String approvalId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "operator_id", length = 50)
    private String operatorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CreditLimitAdjustment() {
    }

    public CreditLimitAdjustment(UUID adjId, UUID accountId, String adjType, BigDecimal oldLimit,
                                   BigDecimal newLimit, String reason, String approvalId,
                                   String status, String operatorId, LocalDateTime createdAt) {
        this.adjId = adjId;
        this.accountId = accountId;
        this.adjType = adjType;
        this.oldLimit = oldLimit;
        this.newLimit = newLimit;
        this.reason = reason;
        this.approvalId = approvalId;
        this.status = status;
        this.operatorId = operatorId;
        this.createdAt = createdAt;
    }

    public UUID getAdjId() {
        return adjId;
    }

    public void setAdjId(UUID adjId) {
        this.adjId = adjId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAdjType() {
        return adjType;
    }

    public void setAdjType(String adjType) {
        this.adjType = adjType;
    }

    public BigDecimal getOldLimit() {
        return oldLimit;
    }

    public void setOldLimit(BigDecimal oldLimit) {
        this.oldLimit = oldLimit;
    }

    public BigDecimal getNewLimit() {
        return newLimit;
    }

    public void setNewLimit(BigDecimal newLimit) {
        this.newLimit = newLimit;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "APPROVED";
    }
}