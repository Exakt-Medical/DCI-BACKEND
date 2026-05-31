package com.exakt.vvip.generateVoucher.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillerooPurchaseRequest {
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
