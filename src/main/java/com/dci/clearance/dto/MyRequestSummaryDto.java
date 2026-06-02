package com.dci.clearance.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MyRequestSummaryDto {
    private Long voucherRequestId;
    private String voucherReferenceNo;
    private String voucherStatus;
    private Long clearanceRequestId;
    private String clearanceReferenceNo;
    private String clearanceStatus;
    private String certificateNo;
    private String plateNumber;
    private String mvFileNumber;
    private String ownerName;
    private String vehicleMake;
    private String vehicleSeries;
    private String clientName;
    private LocalDateTime dateCreated;
}
