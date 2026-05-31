package com.exakt.vvip.generateVoucher.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillerooWebhookRequest {
    private String timestamp;
    private String invoiceReference;
    private BigDecimal invoiceAmount;
    private Integer voucherCount;
    private String companyCode;
}
