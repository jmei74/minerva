package com.minerva.creditcard.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 额度调整请求 DTO
 */
public class LimitAdjustmentRequest {

    @NotNull(message = "newLimit is required")
    @DecimalMin(value = "0", message = "newLimit cannot be negative")
    private BigDecimal newLimit;

    @NotNull(message = "type is required")
    @Pattern(regexp = "PERMANENT|TEMP", message = "type must be PERMANENT or TEMP")
    private String type;

    private String reason;

    private String approvalId;

    private String operatorId;

    public LimitAdjustmentRequest() {
    }

    public LimitAdjustmentRequest(BigDecimal newLimit, String type, String reason,
                                   String approvalId, String operatorId) {
        this.newLimit = newLimit;
        this.type = type;
        this.reason = reason;
        this.approvalId = approvalId;
        this.operatorId = operatorId;
    }

    public BigDecimal getNewLimit() {
        return newLimit;
    }

    public void setNewLimit(BigDecimal newLimit) {
        this.newLimit = newLimit;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal newLimit;
        private String type;
        private String reason;
        private String approvalId;
        private String operatorId;

        public Builder newLimit(BigDecimal newLimit) {
            this.newLimit = newLimit;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder approvalId(String approvalId) {
            this.approvalId = approvalId;
            return this;
        }

        public Builder operatorId(String operatorId) {
            this.operatorId = operatorId;
            return this;
        }

        public LimitAdjustmentRequest build() {
            return new LimitAdjustmentRequest(newLimit, type, reason, approvalId, operatorId);
        }
    }
}