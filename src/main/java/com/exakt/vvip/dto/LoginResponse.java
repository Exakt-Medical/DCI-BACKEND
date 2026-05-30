package com.exakt.vvip.dto;

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
    private String message;
    private Boolean allowedToBuyVoucher;
}