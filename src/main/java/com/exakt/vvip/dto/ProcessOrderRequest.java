package com.exakt.vvip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessOrderRequest {

    @JsonProperty("orderId")
    private Long orderId;
}

