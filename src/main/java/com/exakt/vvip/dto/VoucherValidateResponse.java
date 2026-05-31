package com.exakt.vvip.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoucherValidateResponse {
    private String  voucherCode;
    private String  status;
    private String  expiresAt;
    private String  ownerUsername;
    private long    remainingVouchers; // AVAILABLE count for this user
}