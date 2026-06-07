package com.minerva.creditcard.domain.entity;

import com.minerva.creditcard.domain.enums.AccountStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account",
       indexes = {
           @Index(name = "idx_account_customer", columnList = "customer_id"),
           @Index(name = "idx_account_status", columnList = "status")
       })
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id", updatable = false, nullable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "card_no_encrypted", length = 256, nullable = false)
    private String cardNoEncrypted;

    @Column(name = "card_no_last4", length = 4, nullable = false)
    private String cardNoLast4;

    @Column(name = "credit_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @Column(name = "temp_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal tempLimit;

    @Column(name = "used_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal usedAmount;

    @Column(name = "frozen_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal frozenAmount;

    @Column(name = "billing_day", nullable = false)
    private Integer billingDay;

    @Column(name = "due_days", nullable = false)
    private Integer dueDays;

    @Column(name = "over_limit_ratio", precision = 5, scale = 4, nullable = false)
    private BigDecimal overLimitRatio;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AccountStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Account() {
    }

    public Account(UUID accountId, UUID customerId, String cardNoEncrypted, String cardNoLast4,
                   BigDecimal creditLimit, BigDecimal tempLimit, BigDecimal usedAmount,
                   BigDecimal frozenAmount, Integer billingDay, Integer dueDays,
                   BigDecimal overLimitRatio, AccountStatus status, Long version,
                   LocalDate openDate, LocalDate closeDate, LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.cardNoEncrypted = cardNoEncrypted;
        this.cardNoLast4 = cardNoLast4;
        this.creditLimit = creditLimit;
        this.tempLimit = tempLimit;
        this.usedAmount = usedAmount;
        this.frozenAmount = frozenAmount;
        this.billingDay = billingDay;
        this.dueDays = dueDays;
        this.overLimitRatio = overLimitRatio;
        this.status = status;
        this.version = version;
        this.openDate = openDate;
        this.closeDate = closeDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getCardNoEncrypted() {
        return cardNoEncrypted;
    }

    public void setCardNoEncrypted(String cardNoEncrypted) {
        this.cardNoEncrypted = cardNoEncrypted;
    }

    public String getCardNoLast4() {
        return cardNoLast4;
    }

    public void setCardNoLast4(String cardNoLast4) {
        this.cardNoLast4 = cardNoLast4;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getTempLimit() {
        return tempLimit;
    }

    public void setTempLimit(BigDecimal tempLimit) {
        this.tempLimit = tempLimit;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }

    public BigDecimal getFrozenAmount() {
        return frozenAmount;
    }

    public void setFrozenAmount(BigDecimal frozenAmount) {
        this.frozenAmount = frozenAmount;
    }

    public Integer getBillingDay() {
        return billingDay;
    }

    public void setBillingDay(Integer billingDay) {
        this.billingDay = billingDay;
    }

    public Integer getDueDays() {
        return dueDays;
    }

    public void setDueDays(Integer dueDays) {
        this.dueDays = dueDays;
    }

    public BigDecimal getOverLimitRatio() {
        return overLimitRatio;
    }

    public void setOverLimitRatio(BigDecimal overLimitRatio) {
        this.overLimitRatio = overLimitRatio;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = AccountStatus.PENDING;
        if (creditLimit == null) creditLimit = BigDecimal.ZERO;
        if (tempLimit == null) tempLimit = BigDecimal.ZERO;
        if (usedAmount == null) usedAmount = BigDecimal.ZERO;
        if (frozenAmount == null) frozenAmount = BigDecimal.ZERO;
        if (overLimitRatio == null) overLimitRatio = new BigDecimal("0.1000");
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getAvailableAmount() {
        return creditLimit
                .add(tempLimit)
                .subtract(usedAmount)
                .subtract(frozenAmount);
    }

    public BigDecimal getMaxAllowedAmount() {
        return getAvailableAmount().multiply(BigDecimal.ONE.add(overLimitRatio));
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}