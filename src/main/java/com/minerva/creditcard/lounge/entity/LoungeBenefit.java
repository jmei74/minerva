package com.minerva.creditcard.lounge.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 贵宾室权益表
 * 对应架构文档: §3.2 数据层映射 - LoungeBenefit
 */
@Entity
@Table(name = "lounge_benefit", indexes = {
    @Index(name = "idx_benefit_account", columnList = "account_id"),
    @Index(name = "idx_benefit_account_network", columnList = "account_id, lounge_network", unique = true)
})
public class LoungeBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "benefit_id")
    private UUID benefitId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "lounge_network", nullable = false, length = 50)
    private String loungeNetwork;  // Priority_Pass / LoungeKey / MC_Travel_Pass

    @Column(name = "card_last4", nullable = false, length = 4)
    private String cardLast4;

    @Column(name = "total_uses", nullable = false)
    private Integer totalUses = 0;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BenefitStatus status = BenefitStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum BenefitStatus {
        ACTIVE, EXPIRED, SUSPENDED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getBenefitId() { return benefitId; }
    public void setBenefitId(UUID benefitId) { this.benefitId = benefitId; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public String getLoungeNetwork() { return loungeNetwork; }
    public void setLoungeNetwork(String loungeNetwork) { this.loungeNetwork = loungeNetwork; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }

    public Integer getTotalUses() { return totalUses; }
    public void setTotalUses(Integer totalUses) { this.totalUses = totalUses; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public BenefitStatus getStatus() { return status; }
    public void setStatus(BenefitStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * 计算剩余可用次数
     */
    public int getRemainingUses() {
        return Math.max(0, totalUses - usedCount);
    }

    /**
     * 检查权益是否有效
     */
    public boolean isValid() {
        return status == BenefitStatus.ACTIVE 
            && LocalDate.now().isBefore(expiryDate)
            && getRemainingUses() > 0;
    }
}