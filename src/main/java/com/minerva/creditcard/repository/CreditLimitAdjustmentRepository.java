package com.minerva.creditcard.repository;

import com.minerva.creditcard.domain.entity.CreditLimitAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 额度调整记录 Repository
 */
@Repository
public interface CreditLimitAdjustmentRepository extends JpaRepository<CreditLimitAdjustment, UUID> {

    /**
     * 按账户ID查询调整历史
     */
    List<CreditLimitAdjustment> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
