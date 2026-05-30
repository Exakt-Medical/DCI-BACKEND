package com.exakt.vvip.generateVoucher.dto;

import lombok.Data;

@Data
public class BillerooConfirmResponse {
    private String timestamp;
    private int status;
    private String message;
    private BillerooConfirmData data;

    @Data
    public static class BillerooConfirmData {
        private String description;
        private int voucherCount;
    }
}
