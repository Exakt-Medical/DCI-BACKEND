package com.exakt.vvip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MfaVerifyRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String code;
}