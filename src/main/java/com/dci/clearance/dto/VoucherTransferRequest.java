package com.dci.clearance.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherTransferRequest {
    private Long toUserId;
    private Integer quantity;

    private List<Long> voucherIds;
}