package com.minerva.creditcard.repository;

import com.minerva.creditcard.domain.CreditLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CreditLimitRepository extends JpaRepository<CreditLimit, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.account.id = :accountId")
    Optional<CreditLimit> findByAccountIdForUpdate(@Param("accountId") Long accountId);
    
    Optional<CreditLimit> findByAccountId(Long accountId);
}
