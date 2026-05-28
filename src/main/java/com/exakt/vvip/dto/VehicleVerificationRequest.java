package com.exakt.vvip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleVerificationRequest {

    private String mvFileNumber;
    private String plateNumber;
    private String engineNumber;
    private String chassisNumber;

    private String insuranceCode;
    private String policyNumber;
    private String premiumType;
    private String prescribedPremiumFee;
    private String dst;
    private String vat;
    private String lgt;
    private String validationFee;
    private String totalAmount;
    private String voucherCode;
}