package com.minerva.creditcard.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationResponse {
    private String transactionId;
    private String status;
    private String authorizationCode;
    private BigDecimal authorizedAmount;
    private BigDecimal availableCredit;
    private String message;
    private LocalDateTime timestamp;
}
