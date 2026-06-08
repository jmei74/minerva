package com.minerva.creditcard.lounge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 贵宾室入场记录表
 * 对应架构文档: §3.2 数据层映射 - LoungeAccessRecord
 */
@Entity
@Table(name = "lounge_access_record", indexes = {
    @Index(name = "idx_access_benefit", columnList = "benefit_id"),
    @Index(name = "idx_access_time", columnList = "access_time DESC")
})
public class LoungeAccessRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "access_id")
    private UUID accessId;

    @Column(name = "benefit_id", nullable = false)
    private UUID benefitId;

    @Column(name = "lounge_id", nullable = false, length = 50)
    private String loungeId;

    @Column(name = "lounge_name", length = 200)
    private String loungeName;

    @Column(name = "airport_code", nullable = false, length = 3)
    private String airportCode;

    @Column(name = "access_time", nullable = false)
    private LocalDateTime accessTime;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount = 1;

    @Column(name = "qr_code", nullable = false, length = 500)
    private String qrCode;  // JWT token for QR code

    @Column(name = "qr_valid_from", nullable = false)
    private LocalDateTime qrValidFrom;

    @Column(name = "qr_valid_until", nullable = false)
    private LocalDateTime qrValidUntil;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccessStatus status = AccessStatus.USED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AccessStatus {
        USED, EXPIRED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (accessTime == null) {
            accessTime = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public UUID getAccessId() { return accessId; }
    public void setAccessId(UUID accessId) { this.accessId = accessId; }

    public UUID getBenefitId() { return benefitId; }
    public void setBenefitId(UUID benefitId) { this.benefitId = benefitId; }

    public String getLoungeId() { return loungeId; }
    public void setLoungeId(String loungeId) { this.loungeId = loungeId; }

    public String getLoungeName() { return loungeName; }
    public void setLoungeName(String loungeName) { this.loungeName = loungeName; }

    public String getAirportCode() { return airportCode; }
    public void setAirportCode(String airportCode) { this.airportCode = airportCode; }

    public LocalDateTime getAccessTime() { return accessTime; }
    public void setAccessTime(LocalDateTime accessTime) { this.accessTime = accessTime; }

    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public LocalDateTime getQrValidFrom() { return qrValidFrom; }
    public void setQrValidFrom(LocalDateTime qrValidFrom) { this.qrValidFrom = qrValidFrom; }

    public LocalDateTime getQrValidUntil() { return qrValidUntil; }
    public void setQrValidUntil(LocalDateTime qrValidUntil) { this.qrValidUntil = qrValidUntil; }

    public AccessStatus getStatus() { return status; }
    public void setStatus(AccessStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * 检查 QR 码是否在有效期内
     */
    public boolean isQrValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == AccessStatus.USED
            && now.isAfter(qrValidFrom)
            && now.isBefore(qrValidUntil);
    }
}