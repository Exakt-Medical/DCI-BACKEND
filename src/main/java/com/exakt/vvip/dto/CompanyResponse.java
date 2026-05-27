package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyResponse {
    private Long id;
    private String companyName;
    private String code;
    private String provider;
    private String approvalStatus;
    private String status;
    private String address;
    private String userstamp;
    private String dateCreated;
}
