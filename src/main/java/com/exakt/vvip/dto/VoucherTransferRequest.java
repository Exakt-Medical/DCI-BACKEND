package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherTransferRequest {
    private Long toUserId;   // agent to transfer to
    private Integer quantity; // number of vouchers to transfer
}