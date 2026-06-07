package com.minerva.creditcard.dto;

import com.minerva.creditcard.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 授权响应 DTO
 * 架构参考: architecture-design.md §3.2
 */
public class AuthorizationResponse {

    public String authCode;        // 授权码
    public String referenceNo;    // 参考号
    public String status;          // APPROVED / DECLINED
    public BigDecimal availableAmount;  // 交易后可用额度
    public String declineCode;     // 拒绝码（ declined 时填充）
    public String declineMessage;  // 拒绝原因（ declined 时填充）

    public AuthorizationResponse() {
    }

    public AuthorizationResponse(String authCode, String referenceNo, String status,
                                 BigDecimal availableAmount, String declineCode,
                                 String declineMessage) {
        this.authCode = authCode;
        this.referenceNo = referenceNo;
        this.status = status;
        this.availableAmount = availableAmount;
        this.declineCode = declineCode;
        this.declineMessage = declineMessage;
    }

    public static AuthorizationResponse approved(String authCode, String referenceNo, BigDecimal availableAmount) {
        AuthorizationResponse response = new AuthorizationResponse();
        response.authCode = authCode;
        response.referenceNo = referenceNo;
        response.status = "APPROVED";
        response.availableAmount = availableAmount;
        return response;
    }

    public static AuthorizationResponse declined(String declineCode, String declineMessage) {
        AuthorizationResponse response = new AuthorizationResponse();
        response.status = "DECLINED";
        response.declineCode = declineCode;
        response.declineMessage = declineMessage;
        return response;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(BigDecimal availableAmount) {
        this.availableAmount = availableAmount;
    }

    public String getDeclineCode() {
        return declineCode;
    }

    public void setDeclineCode(String declineCode) {
        this.declineCode = declineCode;
    }

    public String getDeclineMessage() {
        return declineMessage;
    }

    public void setDeclineMessage(String declineMessage) {
        this.declineMessage = declineMessage;
    }
}