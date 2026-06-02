package com.dci.clearance.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CertificateRequest {
    private String mvFileNumber;
    private String plateNumber;
    private String make;
    private String series;
    private String color;
    private String yearModel;
    private String classification;
    private String bodyType;
    private String engineNumber;
    private String chassisNumber;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerMiddleName;
    private String ownerAddress;
    private String ownerContactNo;
    private String ownerEmail;
    private String ownerTin;
    private String policyNumber;
    private String voucherCode;
    private String premiumType;
    private BigDecimal totalAmount;
    private String authNo;
}