package com.exakt.vvip.merchantCallback.dto;

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
public class BilleroPurchaseResult {

    private boolean success;
    private int statusCode;
    private String code;
    private String message;
    private String reference;
    private String merchantReference;
    private String rawError;
    private JsonNode rawResponse;
}
