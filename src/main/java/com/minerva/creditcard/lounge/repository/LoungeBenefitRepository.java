package com.minerva.creditcard.lounge.repository;

import com.minerva.creditcard.lounge.entity.LoungeBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 贵宾室权益 Repository
 */
@Repository
public interface LoungeBenefitRepository extends JpaRepository<LoungeBenefit, UUID> {

    /**
     * 查询账户所有有效权益
     */
    @Query("SELECT lb FROM LoungeBenefit lb WHERE lb.accountId = :accountId AND lb.status = 'ACTIVE'")
    List<LoungeBenefit> findActiveByAccountId(@Param("accountId") UUID accountId);

    /**
     * 查询账户特定网络的权益
     */
    Optional<LoungeBenefit> findByAccountIdAndLoungeNetwork(UUID accountId, String loungeNetwork);

    /**
     * 查询即将过期的权益（7天内）
     */
    @Query("SELECT lb FROM LoungeBenefit lb WHERE lb.accountId = :accountId " +
                     "AND lb.status = 'ACTIVE' AND lb.expiryDate <= :thresholdDate")
        List<LoungeBenefit> findExpiringSoon(@Param("accountId") UUID accountId, @Param("thresholdDate") java.time.LocalDate thresholdDate);
}