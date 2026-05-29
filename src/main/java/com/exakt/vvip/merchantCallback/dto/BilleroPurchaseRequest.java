package com.exakt.vvip.merchantCallback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class BilleroPurchaseRequest {

    private String companyCode;
    private String companyName;
    private String contact;
    private String email;
    private String firstName;
    private String lastName;
    private String reference;
    private BigDecimal totalAmount;
    private Integer voucherCount;
    private BigDecimal voucherFee;
}
