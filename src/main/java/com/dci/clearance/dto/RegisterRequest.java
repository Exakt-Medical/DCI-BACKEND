package com.dci.clearance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String email;
}