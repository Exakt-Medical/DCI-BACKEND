package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    private String companyName;
    private String companyCode;
    private String branch;
    private String provider;
    private String address;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String mobile;
}