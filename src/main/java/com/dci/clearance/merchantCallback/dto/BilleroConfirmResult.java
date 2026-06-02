package com.dci.clearance.merchantCallback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BilleroConfirmResult {

    private boolean success;
    private boolean voucherAlreadyProcessed;
    private Integer statusCode;
    private String message;
    private String error;
    private String rawError;
    private JsonNode rawResponse;
}