package com.minerva.creditcard.lounge.repository;

import com.minerva.creditcard.lounge.entity.LoungeAccessRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 贵宾室入场记录 Repository
 */
@Repository
public interface LoungeAccessRecordRepository extends JpaRepository<LoungeAccessRecord, UUID> {

    /**
     * 查询权益的所有入场记录
     */
    List<LoungeAccessRecord> findByBenefitIdOrderByAccessTimeDesc(UUID benefitId);
}