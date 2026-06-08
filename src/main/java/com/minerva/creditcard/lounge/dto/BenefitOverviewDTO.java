package com.minerva.creditcard.lounge.dto;

import com.minerva.creditcard.lounge.entity.LoungeBenefit;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 权益总览看板 DTO (F1)
 */
public record BenefitOverviewDTO(
    UUID benefitId,
    String loungeNetwork,
    String cardLast4,
    int totalUses,
    int usedCount,
    int remainingUses,
    LocalDate expiryDate,
    boolean isExpiringSoon,
    String status
) {
    public static BenefitOverviewDTO fromEntity(LoungeBenefit entity) {
        LocalDate sevenDaysLater = LocalDate.now().plusDays(7);
        return new BenefitOverviewDTO(
            entity.getBenefitId(),
            entity.getLoungeNetwork(),
            entity.getCardLast4(),
            entity.getTotalUses(),
            entity.getUsedCount(),
            entity.getRemainingUses(),
            entity.getExpiryDate(),
            entity.getExpiryDate().isBefore(sevenDaysLater),
            entity.getStatus().name()
        );
    }
}