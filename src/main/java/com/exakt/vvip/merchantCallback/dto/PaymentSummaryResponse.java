package com.exakt.vvip.merchantCallback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentSummaryResponse {

    private BigDecimal amountPaid;
    private String companyCode;
    private String merchantReference;
    private String paymentReference;
    private String statusCode;
    private Integer voucherCount;
    private BigDecimal voucherFee;
    private String voucherStatusLabel;
    private boolean voucherAlreadyProcessed;
    private String voucherDescription;
    private JsonNode report;
    private JsonNode confirmResponse;
}