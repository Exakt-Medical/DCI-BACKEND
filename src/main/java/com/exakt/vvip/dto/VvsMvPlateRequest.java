package com.exakt.vvip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class VvsMvPlateRequest {
    @JsonProperty("MVFileNo") private String mvFileNo;
    @JsonProperty("PlateNo")  private String plateNo;
}