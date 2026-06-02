package com.dci.clearance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VvsEngineChassisRequest {

    @JsonProperty("engineNumber")
    private String engineNumber;

    @JsonProperty("chassisNumber")
    private String chassisNumber;
}