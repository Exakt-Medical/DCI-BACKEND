package com.exakt.vvip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleVerificationRequest {
    private String mvFileNumber;
    private String plateNumber;
    private String chassisNumber;
    private String engineNumber;
}