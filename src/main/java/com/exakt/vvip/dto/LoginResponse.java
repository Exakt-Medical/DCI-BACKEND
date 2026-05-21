package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private String message;
}