package com.minerva.creditcard.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 创建账户请求 DTO
 */
public class CreateAccountRequest {

    @NotNull(message = "customerId is required")
    private UUID customerId;

    @NotBlank(message = "cardNo is required")
    @Pattern(regexp = "\\d{13,19}", message = "cardNo must be 13-19 digits")
    private String cardNo;

    @NotNull(message = "creditLimit is required")
    @DecimalMin(value = "0.01", message = "creditLimit must be positive")
    private BigDecimal creditLimit;

    @DecimalMin(value = "0", message = "tempLimit cannot be negative")
    private BigDecimal tempLimit;

    @NotNull(message = "billingDay is required")
    @Min(value = 1, message = "billingDay must be between 1 and 31")
    @Max(value = 31, message = "billingDay must be between 1 and 31")
    private Integer billingDay;

    @Min(value = 1, message = "dueDays must be at least 1")
    private Integer dueDays = 20;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(UUID customerId, String cardNo, BigDecimal creditLimit,
                                 BigDecimal tempLimit, Integer billingDay, Integer dueDays) {
        this.customerId = customerId;
        this.cardNo = cardNo;
        this.creditLimit = creditLimit;
        this.tempLimit = tempLimit;
        this.billingDay = billingDay;
        this.dueDays = dueDays;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID customerId;
        private String cardNo;
        private BigDecimal creditLimit;
        private BigDecimal tempLimit;
        private Integer billingDay;
        private Integer dueDays = 20;

        public Builder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder cardNo(String cardNo) {
            this.cardNo = cardNo;
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

        public Builder billingDay(Integer billingDay) {
            this.billingDay = billingDay;
            return this;
        }

        public Builder dueDays(Integer dueDays) {
            this.dueDays = dueDays;
            return this;
        }

        public CreateAccountRequest build() {
            return new CreateAccountRequest(customerId, cardNo, creditLimit, tempLimit,
                    billingDay, dueDays);
        }
    }
}