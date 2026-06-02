package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherValidateRequest {
    private String voucherCode;
}