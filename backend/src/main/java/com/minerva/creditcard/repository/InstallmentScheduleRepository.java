package com.minerva.creditcard.repository;

import com.minerva.creditcard.domain.InstallmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentScheduleRepository extends JpaRepository<InstallmentSchedule, Long> {
    
    Optional<InstallmentSchedule> findByScheduleId(String scheduleId);
    
    List<InstallmentSchedule> findByAccountId(Long accountId);
    
    List<InstallmentSchedule> findByAccountIdAndStatus(Long accountId, InstallmentSchedule.InstallmentStatus status);
    
    @Query("SELECT s FROM InstallmentSchedule s WHERE s.account.id = :accountId AND s.nextPaymentDate <= :dueDate AND s.status = :status")
    List<InstallmentSchedule> findDueInstallments(@Param("accountId") Long accountId,
                                                   @Param("dueDate") LocalDate dueDate,
                                                   @Param("status") InstallmentSchedule.InstallmentStatus status);
}
