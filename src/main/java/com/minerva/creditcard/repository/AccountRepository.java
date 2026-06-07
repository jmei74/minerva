package com.minerva.creditcard.repository;

import com.minerva.creditcard.domain.entity.Account;
import com.minerva.creditcard.domain.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 账户 Repository
 * 架构约束: §3.1 账户创建：先落库再发事件，失败回滚
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * 按卡号后4位查询（唯一索引）
     */
    Optional<Account> findByCardNoLast4(String cardNoLast4);

    /**
     * 按客户ID查询账户列表
     */
    List<Account> findByCustomerId(UUID customerId);

    /**
     * 按状态查询账户
     */
    List<Account> findByStatus(AccountStatus status);

    /**
     * 乐观锁读取账户（用于更新）
     * 架构约束: §3.2 乐观锁（version字段）+ 重试机制（最多3次）
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
    Optional<Account> findByIdForUpdate(@Param("accountId") UUID accountId);
}
