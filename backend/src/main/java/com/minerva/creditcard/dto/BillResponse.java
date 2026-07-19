package com.minerva.creditcard.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillResponse {
    private String billId;
    private Long accountId;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private LocalDate statementDate;
    private LocalDate paymentDueDate;
    private BigDecimal totalAmount;
    private BigDecimal minimumPayment;
    private String status;
    private List<BillItemResponse> items;
}
