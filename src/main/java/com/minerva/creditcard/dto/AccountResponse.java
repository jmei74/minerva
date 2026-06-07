package com.minerva.creditcard.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 账户响应 DTO（敏感数据已掩码）
 * 架构约束: §11 PCI-DSS - 日志/响应中禁止明文卡号
 */
public class AccountResponse {

    public UUID accountId;
    public UUID customerId;
    public String cardNoMasked;     // **** **** **** 1234
    public BigDecimal creditLimit;
    public BigDecimal tempLimit;
    public BigDecimal usedAmount;
    public BigDecimal frozenAmount;
    public BigDecimal availableAmount;
    public Integer billingDay;
    public Integer dueDays;
    public String status;
    public String openDate;
    public String closeDate;
    public String createdAt;

    public AccountResponse() {
    }

    public AccountResponse(UUID accountId, UUID customerId, String cardNoMasked,
                           BigDecimal creditLimit, BigDecimal tempLimit,
                           BigDecimal usedAmount, BigDecimal frozenAmount,
                           BigDecimal availableAmount, Integer billingDay,
                           Integer dueDays, String status, String openDate,
                           String closeDate, String createdAt) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.cardNoMasked = cardNoMasked;
        this.creditLimit = creditLimit;
        this.tempLimit = tempLimit;
        this.usedAmount = usedAmount;
        this.frozenAmount = frozenAmount;
        this.availableAmount = availableAmount;
        this.billingDay = billingDay;
        this.dueDays = dueDays;
        this.status = status;
        this.openDate = openDate;
        this.closeDate = closeDate;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID accountId;
        private UUID customerId;
        private String cardNoMasked;
        private BigDecimal creditLimit;
        private BigDecimal tempLimit;
        private BigDecimal usedAmount;
        private BigDecimal frozenAmount;
        private BigDecimal availableAmount;
        private Integer billingDay;
        private Integer dueDays;
        private String status;
        private String openDate;
        private String closeDate;
        private String createdAt;

        public Builder accountId(UUID accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder cardNoMasked(String cardNoMasked) {
            this.cardNoMasked = cardNoMasked;
            return this;
        }

        public Builder creditLimit(BigDecimal creditLimit) {
            this.creditLimit = creditLimit;
            return this;
        }

        public Builder tempLimit(BigDecimal tempLimit) {
            this.tempLimit = tempLimit;
            return this;
        }

        public Builder usedAmount(BigDecimal usedAmount) {
            this.usedAmount = usedAmount;
            return this;
        }

        public Builder frozenAmount(BigDecimal frozenAmount) {
            this.frozenAmount = frozenAmount;
            return this;
        }

        public Builder availableAmount(BigDecimal availableAmount) {
            this.availableAmount = availableAmount;
            return this;
        }

        public Builder billingDay(Integer billingDay) {
            this.billingDay = billingDay;
            return this;
        }

        public Builder dueDays(Integer dueDays) {
            this.dueDays = dueDays;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder openDate(String openDate) {
            this.openDate = openDate;
            return this;
        }

        public Builder closeDate(String closeDate) {
            this.closeDate = closeDate;
            return this;
        }

        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AccountResponse build() {
            return new AccountResponse(accountId, customerId, cardNoMasked, creditLimit,
                    tempLimit, usedAmount, frozenAmount, availableAmount, billingDay,
                    dueDays, status, openDate, closeDate, createdAt);
        }
    }
}