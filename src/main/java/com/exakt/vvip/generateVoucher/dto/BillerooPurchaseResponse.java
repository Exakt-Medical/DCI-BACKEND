package com.exakt.vvip.generateVoucher.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillerooPurchaseResponse {
    private String timestamp;
    private Integer status;
    private String message;
    private String path;
    private Data data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Data {
        private String code;
        private String message;
        private String timestamp;
        private String reference;
        private String merchantReference;
    }
}
