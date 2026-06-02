package com.dci.clearance.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseResponse {
    private String policyNumber;
    private String voucherCode;
    private String productName;
    private BigDecimal premium;
    private String purchaseDate;
    private String expirationDate;
    private String status;
    private String insuranceCode;
}