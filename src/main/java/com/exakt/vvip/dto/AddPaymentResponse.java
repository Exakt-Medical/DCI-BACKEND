package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddPaymentResponse {
    private Long transactionId;
    private String merchantRefId;
    private String paymentStatus;
    private Long purchaseId;
    private String message;
}
