package com.minerva.creditcard.lounge.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * QR 码验证请求/响应 DTO (F3)
 */
public record AccessValidationDTO(
    UUID accessId,
    UUID benefitId,
    String status,           // VALID, EXPIRED, INVALID, USED
    String loungeName,
    String airportCode,
    int guestCount,
    LocalDateTime accessTime
) {}