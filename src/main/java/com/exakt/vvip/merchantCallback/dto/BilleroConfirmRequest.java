package com.exakt.vvip.merchantCallback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BilleroConfirmRequest {

    private BigDecimal amountPaid;
    private Integer voucherCount;
    private BigDecimal voucherFee;
    private String companyCode;
    private String merchantReference;
    private String paymentReference;
    private String statusCode;
}