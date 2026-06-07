package com.minerva.creditcard.domain.entity;

import com.minerva.creditcard.domain.enums.AuthorizationStatus;
import com.minerva.creditcard.domain.enums.AuthorizationType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "authorization",
       indexes = {
           @Index(name = "idx_auth_account", columnList = "account_id"),
           @Index(name = "idx_auth_expire", columnList = "expire_time")
       })
public class Authorization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "auth_id", updatable = false, nullable = false)
    private UUID authId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "auth_code", length = 20, nullable = false, unique = true)
    private String authCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", length = 20, nullable = false)
    private AuthorizationType authType;

    @Column(name = "auth_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal authAmount;

    @Column(name = "consumed_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal consumedAmount;

    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AuthorizationStatus status;

    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Authorization() {
    }

    public Authorization(UUID authId, UUID accountId, String authCode, AuthorizationType authType,
                         BigDecimal authAmount, BigDecimal consumedAmount, String merchantId,
                         String merchantName, String terminalId, AuthorizationStatus status,
                         LocalDateTime expireTime, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.authId = authId;
        this.accountId = accountId;
        this.authCode = authCode;
        this.authType = authType;
        this.authAmount = authAmount;
        this.consumedAmount = consumedAmount;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.terminalId = terminalId;
        this.status = status;
        this.expireTime = expireTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getAuthId() {
        return authId;
    }

    public void setAuthId(UUID authId) {
        this.authId = authId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public AuthorizationType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthorizationType authType) {
        this.authType = authType;
    }

    public BigDecimal getAuthAmount() {
        return authAmount;
    }

    public void setAuthAmount(BigDecimal authAmount) {
        this.authAmount = authAmount;
    }

    public BigDecimal getConsumedAmount() {
        return consumedAmount;
    }

    public void setConsumedAmount(BigDecimal consumedAmount) {
        this.consumedAmount = consumedAmount;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public AuthorizationStatus getStatus() {
        return status;
    }

    public void setStatus(AuthorizationStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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
        if (consumedAmount == null) consumedAmount = BigDecimal.ZERO;
        if (status == null) status = AuthorizationStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getReversibleAmount() {
        return authAmount.subtract(consumedAmount);
    }

    public boolean isFullyConsumed() {
        return consumedAmount.compareTo(authAmount) >= 0;
    }
}