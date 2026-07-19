package com.minerva.creditcard.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentRequest {
    
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
    
    @NotNull(message = "Installment count is required")
    @Min(value = 2, message = "Installment count must be at least 2")
    @Max(value = 36, message = "Installment count cannot exceed 36")
    private Integer installmentCount;
    
    @NotNull(message = "APR is required")
    @DecimalMin(value = "0.0", message = "APR must be non-negative")
    @DecimalMax(value = "1.0", message = "APR must not exceed 100%")
    private BigDecimal apr;
}
