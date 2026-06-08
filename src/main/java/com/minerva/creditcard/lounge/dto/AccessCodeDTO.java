package com.minerva.creditcard.lounge.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * QR 码入场凭证 DTO (F3)
 */
public record AccessCodeDTO(
    UUID accessId,
    String qrCode, // JWT token for QR display
    LocalDateTime validFrom,
    LocalDateTime validUntil,
    String loungeName,
    String airportCode,
    int guestCount
) {
    /**
     * 生成有效期内的 QR 码内容（用于扫码验证）
     * 格式: {accessId}:{benefitId}:{timestamp}
     */
    public String getQrContent() {
        return String.format("%s:%s", accessId, validUntil.toString());
    }
}