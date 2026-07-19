package com.minerva.creditcard.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotBlank(message = "Account type is required")
    private String accountType;
    
    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Credit limit must be positive")
    private BigDecimal creditLimit;
    
    @Min(value = 1, message = "Billing cycle day must be between 1 and 31")
    @Max(value = 31, message = "Billing cycle day must be between 1 and 31")
    private Integer billingCycleDay;
}
