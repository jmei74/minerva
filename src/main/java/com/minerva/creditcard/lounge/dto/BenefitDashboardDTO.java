package com.minerva.creditcard.lounge.dto;

import java.util.List;
import java.util.UUID;

/**
 * 权益看板响应 DTO (F1)
 */
public record BenefitDashboardDTO(
    UUID accountId,
    List<BenefitOverviewDTO> benefits,
    int totalLounges,
    int totalRemainingVisits,
    List<BenefitOverviewDTO> expiringSoon
) {}