package com.minerva.creditcard.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentResponse {
    private String scheduleId;
    private String status;
    private BigDecimal principalAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalAmount;
    private BigDecimal monthlyPayment;
    private Integer installmentCount;
    private Integer installmentsRemaining;
    private BigDecimal remainingPrincipal;
    private LocalDate startDate;
    private LocalDate nextPaymentDate;
}
