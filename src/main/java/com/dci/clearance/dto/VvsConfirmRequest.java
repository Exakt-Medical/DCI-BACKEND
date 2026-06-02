package com.dci.clearance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class VvsConfirmRequest {
    @JsonProperty("requestID")  private String requestId;
    @JsonProperty("vvip")       private String vvip;
    @JsonProperty("expiryDate") private String expiryDate;
}