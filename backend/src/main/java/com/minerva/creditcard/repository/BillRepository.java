package com.minerva.creditcard.repository;

import com.minerva.creditcard.domain.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    
    Optional<Bill> findByBillId(String billId);
    
    List<Bill> findByAccountIdOrderByStatementDateDesc(Long accountId);
    
    @Query("SELECT b FROM Bill b WHERE b.account.id = :accountId AND b.status = :status")
    List<Bill> findByAccountIdAndStatus(@Param("accountId") Long accountId, 
                                         @Param("status") Bill.BillStatus status);
    
    @Query("SELECT b FROM Bill b WHERE b.account.accountNo = :accountNo AND b.statementDate BETWEEN :startDate AND :endDate")
    Optional<Bill> findByAccountNoAndStatementDateBetween(@Param("accountNo") String accountNo,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b FROM Bill b WHERE b.status IN :statuses ORDER BY b.account.id, b.statementDate DESC")
    List<Bill> findByStatusIn(@Param("statuses") List<Bill.BillStatus> statuses);
}
