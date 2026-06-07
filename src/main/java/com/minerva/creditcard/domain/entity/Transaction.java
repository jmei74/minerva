package com.minerva.creditcard.domain.entity;

import com.minerva.creditcard.domain.enums.TransactionStatus;
import com.minerva.creditcard.domain.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction",
       indexes = {
           @Index(name = "idx_txn_account_time", columnList = "account_id, txn_time DESC"),
           @Index(name = "idx_txn_auth_code", columnList = "auth_code"),
           @Index(name = "idx_txn_idempotency", columnList = "idempotency_key"),
           @Index(name = "idx_txn_reference", columnList = "reference_no"),
           @Index(name = "idx_txn_settlement", columnList = "settlement_date")
       })
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "txn_id", updatable = false, nullable = false)
    private UUID txnId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", length = 30, nullable = false)
    private TransactionType txnType;

    @Column(name = "txn_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal txnAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "available_amount_before", precision = 15, scale = 2, nullable = false)
    private BigDecimal availableAmountBefore;

    @Column(name = "available_amount_after", precision = 15, scale = 2, nullable = false)
    private BigDecimal availableAmountAfter;

    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    @Column(name = "auth_code", length = 20)
    private String authCode;

    @Column(name = "reference_no", length = 50)
    private String referenceNo;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "txn_time", nullable = false)
    private LocalDateTime txnTime;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Transaction() {
    }

    public Transaction(UUID txnId, UUID accountId, TransactionType txnType, BigDecimal txnAmount,
                       String currency, BigDecimal availableAmountBefore,
                       BigDecimal availableAmountAfter, String merchantId, String merchantName,
                       String terminalId, String authCode, String referenceNo,
                       String idempotencyKey, LocalDateTime txnTime, LocalDate settlementDate,
                       TransactionStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.txnId = txnId;
        this.accountId = accountId;
        this.txnType = txnType;
        this.txnAmount = txnAmount;
        this.currency = currency;
        this.availableAmountBefore = availableAmountBefore;
        this.availableAmountAfter = availableAmountAfter;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.terminalId = terminalId;
        this.authCode = authCode;
        this.referenceNo = referenceNo;
        this.idempotencyKey = idempotencyKey;
        this.txnTime = txnTime;
        this.settlementDate = settlementDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getTxnId() {
        return txnId;
    }

    public void setTxnId(UUID txnId) {
        this.txnId = txnId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public TransactionType getTxnType() {
        return txnType;
    }

    public void setTxnType(TransactionType txnType) {
        this.txnType = txnType;
    }

    public BigDecimal getTxnAmount() {
        return txnAmount;
    }

    public void setTxnAmount(BigDecimal txnAmount) {
        this.txnAmount = txnAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getAvailableAmountBefore() {
        return availableAmountBefore;
    }

    public void setAvailableAmountBefore(BigDecimal availableAmountBefore) {
        this.availableAmountBefore = availableAmountBefore;
    }

    public BigDecimal getAvailableAmountAfter() {
        return availableAmountAfter;
    }

    public void setAvailableAmountAfter(BigDecimal availableAmountAfter) {
        this.availableAmountAfter = availableAmountAfter;
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

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getTxnTime() {
        return txnTime;
    }

    public void setTxnTime(LocalDateTime txnTime) {
        this.txnTime = txnTime;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
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
        if (currency == null) currency = "CNY";
        if (status == null) status = TransactionStatus.PENDING;
        if (txnTime == null) txnTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}