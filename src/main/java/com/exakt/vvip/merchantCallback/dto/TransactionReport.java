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
public class TransactionReport {

    private boolean success;
    private String transactionId;
    private BigDecimal amountPaid;
    private String merchantReference;
    private String paymentReference;
    private String companyCode;
    private String companyName;
    private Integer voucherCount;
    private BigDecimal voucherFee;
    private String statusCode;
    private String voucherDescription;
    private String message;
    private String rawError;
    private JsonNode rawResponse;
    private String firstName;
    private String lastName;
    private String contactMobile;
    private String email;
}