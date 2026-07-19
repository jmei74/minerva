package com.minerva.creditcard.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    private Long id;
    private String accountNo;
    private Long customerId;
    private String accountType;
    private String status;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal currentBalance;
    private Integer billingCycleDay;
    private String token;
    private LocalDateTime createdAt;
}
