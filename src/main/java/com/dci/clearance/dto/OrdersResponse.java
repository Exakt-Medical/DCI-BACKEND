package com.dci.clearance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdersResponse {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("merchant_reference_id")
    private String merchantReferenceId;

    @JsonProperty("invoice_reference")
    private String invoiceReference;

    private String link;

    private String status;
}
