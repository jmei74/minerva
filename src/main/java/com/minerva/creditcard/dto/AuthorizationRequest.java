package com.minerva.creditcard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 授权请求 DTO
 * 架构参考: architecture-design.md §3.2
 */
public class AuthorizationRequest {

    /**
     * 卡号（明文，网关已脱敏后传入）
     */
    @NotBlank(message = "cardNo is required")
    private String cardNo;

    /**
     * 授权金额
     */
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private BigDecimal amount;

    /**
     * 商户ID
     */
    private String merchantId;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 终端ID
     */
    private String terminalId;

    /**
     * 交易类型：PURCHASE / WITHDRAWAL / PRE_AUTH
     */
    @Pattern(regexp = "PURCHASE|WITHDRAWAL|PRE_AUTH", message = "txnType must be PURCHASE/WITHDRAWAL/PRE_AUTH")
    private String txnType;

    /**
     * 幂等键（可选，由网关生成）
     */
    private String idempotencyKey;

    public AuthorizationRequest() {
    }

    public AuthorizationRequest(String cardNo, BigDecimal amount, String merchantId,
                                 String merchantName, String terminalId, String txnType,
                                 String idempotencyKey) {
        this.cardNo = cardNo;
        this.amount = amount;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.terminalId = terminalId;
        this.txnType = txnType;
        this.idempotencyKey = idempotencyKey;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String cardNo;
        private BigDecimal amount;
        private String merchantId;
        private String merchantName;
        private String terminalId;
        private String txnType;
        private String idempotencyKey;

        public Builder cardNo(String cardNo) {
            this.cardNo = cardNo;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder merchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public Builder terminalId(String terminalId) {
            this.terminalId = terminalId;
            return this;
        }

        public Builder txnType(String txnType) {
            this.txnType = txnType;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public AuthorizationRequest build() {
            return new AuthorizationRequest(cardNo, amount, merchantId, merchantName,
                    terminalId, txnType, idempotencyKey);
        }
    }
}