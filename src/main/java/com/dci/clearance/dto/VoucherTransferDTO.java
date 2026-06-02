package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherTransferDTO {
    private Long id;
    private String voucherCode;
    private Long companyId;
    private String companyCode;
    private Long orderId;
    private String tlpeTransactionId;
    private String merchantReference;
    private String paymentReference;
    private Long originalUserId;
    private Long currentUserId;
    private String status;
    private String voucherReference;
    private String redeemedAt;
    private String createdAt;
    private String updatedAt;
    private String expiresAt;
}