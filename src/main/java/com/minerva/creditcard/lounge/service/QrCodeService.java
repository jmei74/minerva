package com.minerva.creditcard.lounge.service;

import com.minerva.creditcard.config.JwtTokenProvider;
import com.minerva.creditcard.lounge.dto.AccessCodeDTO;
import com.minerva.creditcard.lounge.dto.AccessValidationDTO;
import com.minerva.creditcard.lounge.entity.LoungeAccessRecord;
import com.minerva.creditcard.lounge.entity.LoungeBenefit;
import com.minerva.creditcard.lounge.repository.LoungeAccessRecordRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * QR 码服务 - F3 电子通行证
 * 架构参考: SingPass 动态码方案，HMAC-SHA256 签名
 */
@Service
public class QrCodeService {

    private static final Logger log = LoggerFactory.getLogger(QrCodeService.class);
    private static final int QR_VALIDITY_MINUTES = 5;  // 5分钟刷新

    private final LoungeAccessRecordRepository accessRecordRepository;
    private final LoungeBenefitService benefitService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${lounge.qr.key:default-qr-key-change-in-production}")
    private String qrSigningKey;

    public QrCodeService(
            LoungeAccessRecordRepository accessRecordRepository,
            LoungeBenefitService benefitService,
            JwtTokenProvider jwtTokenProvider) {
        this.accessRecordRepository = accessRecordRepository;
        this.benefitService = benefitService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * F3: 生成电子通行证 QR 码
     */
    @Transactional
    public AccessCodeDTO generateAccessCode(UUID benefitId, String loungeId, 
                                            String loungeName, String airportCode, int guestCount) {
        LoungeBenefit benefit = benefitService.getBenefitById(benefitId);

        if (!benefit.isValid()) {
            throw new IllegalStateException("Cannot generate access code: benefit is not valid");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validUntil = now.plusMinutes(QR_VALIDITY_MINUTES);

        // 生成 JWT 签名 QR 码
        String qrCode = generateQrJwt(benefitId, loungeId, validUntil);

        // 保存入场记录
        LoungeAccessRecord record = new LoungeAccessRecord();
        record.setBenefitId(benefitId);
        record.setLoungeId(loungeId);
        record.setLoungeName(loungeName);
        record.setAirportCode(airportCode);
        record.setAccessTime(now);
        record.setGuestCount(guestCount);
        record.setQrCode(qrCode);
        record.setQrValidFrom(now);
        record.setQrValidUntil(validUntil);
        record.setStatus(LoungeAccessRecord.AccessStatus.USED);

        LoungeAccessRecord saved = accessRecordRepository.save(record);
        log.info("Generated access code {} for benefit {}", saved.getAccessId(), benefitId);

        return new AccessCodeDTO(
                saved.getAccessId(),
                qrCode,
                saved.getQrValidFrom(),
                saved.getQrValidUntil(),
                loungeName,
                airportCode,
                guestCount
        );
    }

    /**
     * 验证 QR 码（扫码端）
     */
    @Transactional(readOnly = true)
    public AccessValidationDTO validateAccessCode(String qrCodeJwt) {
        try {
            // 解析 JWT
            Map<String, Object> claims = parseQrJwt(qrCodeJwt);

            UUID accessId = UUID.fromString((String) claims.get("accessId"));
            UUID benefitId = UUID.fromString((String) claims.get("benefitId"));
            LocalDateTime validUntil = LocalDateTime.parse((String) claims.get("validUntil"));

            LoungeAccessRecord record = accessRecordRepository.findById(accessId)
                    .orElse(null);

            if (record == null) {
                return new AccessValidationDTO(
                        accessId, benefitId, "INVALID", null, null, 0, null);
            }

            LocalDateTime now = LocalDateTime.now();

            // 检查状态
            if (record.getStatus() != LoungeAccessRecord.AccessStatus.USED) {
                return new AccessValidationDTO(
                        accessId, benefitId, record.getStatus().name(),
                        record.getLoungeName(), record.getAirportCode(),
                        record.getGuestCount(), record.getAccessTime());
            }

            if (now.isAfter(validUntil)) {
                return new AccessValidationDTO(
                        accessId, benefitId, "EXPIRED",
                        record.getLoungeName(), record.getAirportCode(),
                        record.getGuestCount(), record.getAccessTime());
            }

            //验证权益状态
            LoungeBenefit benefit = benefitService.getBenefitById(benefitId);
            if (!benefit.isValid()) {
                return new AccessValidationDTO(
                        accessId, benefitId, "INVALID_BENEFIT",
                        record.getLoungeName(), record.getAirportCode(),
                        record.getGuestCount(), record.getAccessTime());
            }

            return new AccessValidationDTO(
                    accessId, benefitId, "VALID",
                    record.getLoungeName(), record.getAirportCode(),
                    record.getGuestCount(), record.getAccessTime());

        } catch (Exception e) {
            log.warn("QR validation failed: {}", e.getMessage());
            return new AccessValidationDTO(
                    null, null, "INVALID", null, null, 0, null);
        }
    }

    /**
     * 生成 QR 码 JWT (HMAC-SHA256签名)
     */
    private String generateQrJwt(UUID benefitId, String loungeId, LocalDateTime validUntil) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accessId", UUID.randomUUID().toString());
        claims.put("benefitId", benefitId.toString());
        claims.put("loungeId", loungeId);
        claims.put("validUntil", validUntil.toString());
        claims.put("type", "lounge_access");
        claims.put("iat", System.currentTimeMillis() / 1000);

        SecretKey key = Keys.hmacShaKeyFor(qrSigningKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 QR 码 JWT
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseQrJwt(String token) {
        SecretKey key = Keys.hmacShaKeyFor(qrSigningKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}