package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddPaymentRequest {
    private Long transactionId;
    private String merchantRefId;
    private String paymentStatus;
    private Long purchaseId;
}
