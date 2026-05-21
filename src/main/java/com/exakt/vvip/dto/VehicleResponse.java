package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleResponse {
    private String mvFileNumber;
    private String plateNumber;
    private String engineNumber;
    private String chassisNumber;
    private String make;
    private String series;
    private String color;
    private String yearModel;
    private String classification;
    private String bodyType;
    private String vehicleCategory;
    private String vehicleType;
    private String lastRegistrationDate;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerMiddleName;
    private String ownerAddress;
    private String ownerContactNo;
    private String ownerEmail;
    private String ownerTin;
}