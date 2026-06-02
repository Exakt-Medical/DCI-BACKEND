package com.dci.clearance.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferHistoryDTO {
    private String referenceNumber;
    private Long fromUserId;
    private Long toUserId;
    private String toAgentName;     // stored at transfer time, no extra DB call
    private int quantity;
    private long totalValue;        // quantity × VOUCHER_VALUE, computed in service
    private List<String> voucherCodes;
    private String transferredAt;
    private String status;          // always "Completed"
}