package com.exakt.vvip.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherTransferLogDTO {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Long voucherId;
    private String voucherCode;
    private String transferredAt;
    private String referenceNumber;
}