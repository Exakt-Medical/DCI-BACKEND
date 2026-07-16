package com.dci.clearance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OtpRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String otpCode;
}
