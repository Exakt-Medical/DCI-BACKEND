package com.dci.clearance.generateVoucher.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillerooCompanySyncRequest {
    private List<Data> data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Data {
        private String code;
        private String email;
        private String name;
        private int status;
    }
}
