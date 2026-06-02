package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleSearchRequest {
    private String mvFileNo;
    private String plateNo;
    private String engineNo;
    private String chassisNo;
}