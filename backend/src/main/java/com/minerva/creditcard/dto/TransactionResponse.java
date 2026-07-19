package com.minerva.creditcard.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private String transactionId;
    private String transactionType;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String merchantCategoryCode;
    private String description;
    private String referenceId;
    private String authorizationCode;
    private Integer installmentCount;
    private LocalDateTime createdAt;
}
