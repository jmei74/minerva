package com.minerva.creditcard.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 额度快照（Redis缓存数据结构）
 * 架构参考: architecture-design.md §6.2
 */
public class CreditSnapshot {

    public UUID accountId;
    public BigDecimal creditLimit;
    public BigDecimal tempLimit;
    public BigDecimal usedAmount;
    public BigDecimal frozenAmount;
    public BigDecimal availableAmount;
    public Long version;  // 用于Cache-Aside一致性校验

    public CreditSnapshot() {
    }

    public CreditSnapshot(UUID accountId, BigDecimal creditLimit, BigDecimal tempLimit,
                          BigDecimal usedAmount, BigDecimal frozenAmount,
                          BigDecimal availableAmount, Long version) {
        this.accountId = accountId;
        this.creditLimit = creditLimit;
        this.tempLimit = tempLimit;
        this.usedAmount = usedAmount;
        this.frozenAmount = frozenAmount;
        this.availableAmount = availableAmount;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID accountId;
        private BigDecimal creditLimit;
        private BigDecimal tempLimit;
        private BigDecimal usedAmount;
        private BigDecimal frozenAmount;
        private BigDecimal availableAmount;
        private Long version;

        public Builder accountId(UUID accountId) {
            this.accountId = accountId;
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

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public CreditSnapshot build() {
            return new CreditSnapshot(accountId, creditLimit, tempLimit, usedAmount,
                    frozenAmount, availableAmount, version);
        }
    }
}