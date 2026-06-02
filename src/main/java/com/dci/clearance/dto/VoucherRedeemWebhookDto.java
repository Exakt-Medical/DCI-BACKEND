package com.dci.clearance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VoucherRedeemWebhookDto {

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("transactionReference")
    private String transactionReference;

    @JsonProperty("companyCode")
    private String companyCode;

    @JsonProperty("voucherAmount")
    private Double voucherAmount;

    @JsonProperty("voucherReference")
    private String voucherReference;

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("statusDescription")
    private String statusDescription;
}