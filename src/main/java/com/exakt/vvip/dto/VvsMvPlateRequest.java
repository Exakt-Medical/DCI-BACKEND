package com.exakt.vvip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VvsMvPlateRequest {

    @JsonProperty("MVFileNumber")
    private String mvFileNumber;

    @JsonProperty("PlateNumber")
    private String plateNumber;
}