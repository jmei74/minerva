package com.minerva.creditcard.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItemResponse {
    private Long id;
    private String description;
    private LocalDateTime transactionDate;
    private BigDecimal amount;
    private String itemType;
    private Boolean isInstallment;
    private Integer installmentNumber;
    private Integer totalInstallments;
}
