package com.exakt.vvip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class VvsEngineChassisRequest {
    @JsonProperty("EngineNo")  private String engineNo;
    @JsonProperty("ChassisNo") private String chassisNo;
}