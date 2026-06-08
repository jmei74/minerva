package com.minerva.creditcard.lounge.service;

import com.minerva.creditcard.lounge.dto.*;
import com.minerva.creditcard.lounge.entity.LoungeBenefit;
import com.minerva.creditcard.lounge.entity.LoungeAccessRecord;
import com.minerva.creditcard.lounge.repository.LoungeBenefitRepository;
import com.minerva.creditcard.lounge.repository.LoungeAccessRecordRepository;
import com.minerva.creditcard.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 贵宾室权益服务
 * 对应架构文档: F1 权益总览看板
 */
@Service
public class LoungeBenefitService {

    private static final Logger log = LoggerFactory.getLogger(LoungeBenefitService.class);

    private final LoungeBenefitRepository benefitRepository;
    private final LoungeAccessRecordRepository accessRecordRepository;
    private final QrCodeService qrCodeService;

    public LoungeBenefitService(
            LoungeBenefitRepository benefitRepository,
            LoungeAccessRecordRepository accessRecordRepository,
            @Lazy QrCodeService qrCodeService) {
        this.benefitRepository = benefitRepository;
        this.accessRecordRepository = accessRecordRepository;
        this.qrCodeService = qrCodeService;
    }

    /**
     * F1: 获取权益总览看板
     */
    @Transactional(readOnly = true)
    public BenefitDashboardDTO getBenefitDashboard(UUID accountId) {
        log.info("Fetching benefit dashboard for account: {}", accountId);

        List<LoungeBenefit> activeBenefits = benefitRepository.findActiveByAccountId(accountId);
        List<LoungeBenefit> expiringSoon = benefitRepository.findExpiringSoon(accountId, java.time.LocalDate.now().plusDays(7));

        List<BenefitOverviewDTO> benefitDtos = activeBenefits.stream()
                .map(BenefitOverviewDTO::fromEntity)
                .toList();

        List<BenefitOverviewDTO> expiringSoonDtos = expiringSoon.stream()
                .map(BenefitOverviewDTO::fromEntity)
                .toList();

        int totalRemaining = benefitDtos.stream()
                .mapToInt(BenefitOverviewDTO::remainingUses)
                .sum();

        return new BenefitDashboardDTO(
                accountId,
                benefitDtos,
                benefitDtos.size(),
                totalRemaining,
                expiringSoonDtos
        );
    }

    /**
     * 获取单个权益详情
     */
    @Transactional(readOnly = true)
    public LoungeBenefit getBenefitById(UUID benefitId) {
        return benefitRepository.findById(benefitId)
                .orElseThrow(() -> new ResourceNotFoundException("LoungeBenefit", benefitId));
    }

    /**
     * 使用权益次数（入场时调用）
     */
    @Transactional
    public void useBenefit(UUID benefitId) {
        LoungeBenefit benefit = getBenefitById(benefitId);
        if (!benefit.isValid()) {
            throw new IllegalStateException("Benefit is not valid for use: " + benefitId);
        }
        benefit.setUsedCount(benefit.getUsedCount() + 1);
        benefitRepository.save(benefit);
        log.info("Benefit {} used, remaining: {}", benefitId, benefit.getRemainingUses());
    }

    /**
     * 获取权益的入场历史
     */
    @Transactional(readOnly = true)
    public List<LoungeAccessRecord> getAccessHistory(UUID benefitId) {
        return accessRecordRepository.findByBenefitIdOrderByAccessTimeDesc(benefitId);
    }
}