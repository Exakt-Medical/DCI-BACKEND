package com.exakt.vvip.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherTransferRequest {
    private Long toUserId;           // agent to transfer to
    private Integer quantity;         // fallback: number of vouchers to transfer
    // ✅ Specific voucher IDs to transfer
    private List<Long> voucherIds;
}