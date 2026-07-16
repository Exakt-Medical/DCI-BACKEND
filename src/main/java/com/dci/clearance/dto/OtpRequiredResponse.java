package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpRequiredResponse {
    private String userId;
    private String email;
    private String message;
    private boolean otpRequired;
}
