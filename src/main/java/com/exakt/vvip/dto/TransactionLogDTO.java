package com.exakt.vvip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLogDTO {
    private Long id;
    private String referenceNo;
    private String account;
    private String company;
    private String description;
    private String response;
    private String origin;
    private String status;
    private String dateCreated;
}