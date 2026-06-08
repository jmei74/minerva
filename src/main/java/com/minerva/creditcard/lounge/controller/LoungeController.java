package com.minerva.creditcard.lounge.controller;

import com.minerva.creditcard.lounge.dto.*;
import com.minerva.creditcard.lounge.entity.LoungeAccessRecord;
import com.minerva.creditcard.lounge.service.LoungeBenefitService;
import com.minerva.creditcard.lounge.service.QrCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 贵宾室权益 REST API
 * 对应架构文档: §2.4 API 网关映射
 * 
 * 限流策略:
 * - /v1/lounge/benefits/&#42;: 200 req/min
 * - /v1/lounge/search: 500 req/min
 * - /v1/lounge/&#42;/access-code: 100 req/min
 * - /v1/lounge/access/validate: 无限制（扫码端）
 */
@RestController
@RequestMapping("/v1/lounge")
public class LoungeController {

    private final LoungeBenefitService benefitService;
    private final QrCodeService qrCodeService;

    public LoungeController(LoungeBenefitService benefitService, QrCodeService qrCodeService) {
        this.benefitService = benefitService;
        this.qrCodeService = qrCodeService;
    }

    /**
     * F1: 权益总览看板
     * GET /v1/lounge/benefits
     */
    @GetMapping("/benefits")
    public ResponseEntity<BenefitDashboardDTO> getBenefitDashboard(
            @RequestHeader("X-Account-Id") UUID accountId) {
        BenefitDashboardDTO dashboard = benefitService.getBenefitDashboard(accountId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * F3: 生成电子通行证 QR 码
     * POST /v1/lounge/{benefitId}/access-code
     */
    @PostMapping("/{benefitId}/access-code")
    public ResponseEntity<AccessCodeDTO> generateAccessCode(
            @PathVariable UUID benefitId,
            @RequestBody AccessCodeRequest request) {
        AccessCodeDTO accessCode = qrCodeService.generateAccessCode(
                benefitId,
                request.loungeId(),
                request.loungeName(),
                request.airportCode(),
                request.guestCount()
        );
        return ResponseEntity.ok(accessCode);
    }

    /**
     * F3: 扫码验证 QR 码（扫码端）
     * POST /v1/lounge/access/validate
     */
    @PostMapping("/access/validate")
    public ResponseEntity<AccessValidationDTO> validateAccessCode(
            @RequestBody ValidateAccessRequest request) {
        AccessValidationDTO result = qrCodeService.validateAccessCode(request.qrCode());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取权益入场历史
     * GET /v1/lounge/{benefitId}/history
     */
    @GetMapping("/{benefitId}/history")
    public ResponseEntity<List<LoungeAccessRecord>> getAccessHistory(
            @PathVariable UUID benefitId) {
        List<LoungeAccessRecord> history = benefitService.getAccessHistory(benefitId);
        return ResponseEntity.ok(history);
    }

    // Request DTOs
    public record AccessCodeRequest(
            String loungeId,
            String loungeName,
            String airportCode,
            int guestCount
    ) {}

    public record ValidateAccessRequest(
            String qrCode
    ) {}
}