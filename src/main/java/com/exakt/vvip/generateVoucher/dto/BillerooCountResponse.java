package com.exakt.vvip.generateVoucher.dto;

import lombok.Data;

@Data
public class BillerooCountResponse {
    private String timestamp;
    private int status;
    private String message;
    private BillerooCountData data;

    @Data
    public static class BillerooCountData {
        private int available;
        private int redeemed;
        private int cancelled;
        private int total;
    }
}
