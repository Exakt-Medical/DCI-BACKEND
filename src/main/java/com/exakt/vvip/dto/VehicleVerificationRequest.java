package com.exakt.vvip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleVerificationRequest {
    @NotBlank(message = "MV File Number is required")
    private String mvFileNumber;

    @NotBlank(message = "Plate Number is required")
    private String plateNumber;

    @NotBlank(message = "Engine Number is required")
    private String engineNumber;

    @NotBlank(message = "Chassis Number is required")
    private String chassisNumber;
}