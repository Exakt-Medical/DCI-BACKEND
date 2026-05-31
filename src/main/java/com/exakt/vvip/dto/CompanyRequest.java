package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CompanyRequest {
    private String companyName;
    private String email;
    private String code;
    private String provider;
    private String approvalStatus;
    private String status;
    private String address;
}
