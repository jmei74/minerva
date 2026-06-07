package com.minerva.creditcard.repository;

import com.minerva.creditcard.domain.entity.Transaction;
import com.minerva.creditcard.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 交易流水 Repository
 * 架构约束: §3.2 幂等：基于auth_code/reference_no做幂等校验
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * 按幂等键查询（防重复扣款）
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * 按授权码查询
     */
    Optional<Transaction> findByAuthCode(String authCode);

    /**
     * 按账户ID+时间范围查询交易
     */
    List<Transaction> findByAccountIdAndTxnTimeBetweenOrderByTxnTimeDesc(
            UUID accountId, LocalDateTime start, LocalDateTime end);

    /**
     * 按账户ID查询最新交易
     */
    List<Transaction> findByAccountIdOrderByTxnTimeDesc(UUID accountId);

    /**
     * 按参考号查询
     */
    Optional<Transaction> findByReferenceNo(String referenceNo);

    /**
     * 按账户ID+状态查询
     */
    List<Transaction> findByAccountIdAndStatus(UUID accountId, TransactionStatus status);
}
