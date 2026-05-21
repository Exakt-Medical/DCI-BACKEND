package com.exakt.vvip.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InsuranceFeeResponse {
    private String insuranceCode;
    private BigDecimal prescribedPremiumFee;
    private BigDecimal dst;
    private BigDecimal vat;
    private BigDecimal lgt;
    private BigDecimal validationFee;
    private BigDecimal totalAmount;
}