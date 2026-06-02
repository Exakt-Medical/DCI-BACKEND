package com.dci.clearance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseResponseDTO {
    private Long id;
    private String policyNumber;
    private String voucherCode;
    private String productName;
    private BigDecimal premium;
    private LocalDateTime purchaseDate;
    private LocalDateTime expirationDate;
    private String status;
    private LocalDateTime redeemedOn;
    private String insuranceCode;
}