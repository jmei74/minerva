package com.minerva.creditcard.repository;

import com.minerva.creditcard.domain.entity.Authorization;
import com.minerva.creditcard.domain.enums.AuthorizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 授权表 Repository
 * 架构约束: §3.2 预授权管理
 */
@Repository
public interface AuthorizationRepository extends JpaRepository<Authorization, UUID> {

    /**
     * 按授权码查询
     */
    Optional<Authorization> findByAuthCode(String authCode);

    /**
     * 按账户ID查询
     */
    List<Authorization> findByAccountId(UUID accountId);

    /**
     * 查询待处理的预授权（用于清理过期预授权）
     */
    @Query("SELECT a FROM Authorization a WHERE a.status = :status AND a.expireTime < :now")
    List<Authorization> findExpiredPending(
            @Param("status") AuthorizationStatus status,
            @Param("now") LocalDateTime now);
}
