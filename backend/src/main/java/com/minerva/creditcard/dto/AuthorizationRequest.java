package com.minerva.creditcard.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;
    
    private String currency;
    
    private String merchantName;
    
    private String merchantCategoryCode;
    
    private Integer installmentCount;
    
    private String description;
}
