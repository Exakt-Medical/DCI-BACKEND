package com.dci.clearance.generateVoucher.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class BillerooConfirmRequest {
    private BigDecimal amountPaid;
    private String companyCode;
    private String merchantReference;
    private String paymentReference;
    private String statusCode;
    private int voucherCount;
    private BigDecimal voucherFee;
}
