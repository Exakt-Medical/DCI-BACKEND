package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private Long userId;
    private String token;
    private String email;
    private String firstname;
    private String lastname;
    private String username;
    private String role;
    private String companyCode;
    private String branchRef;
    private String message;
}