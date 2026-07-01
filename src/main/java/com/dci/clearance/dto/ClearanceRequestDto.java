package com.dci.clearance.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClearanceRequestDto {
    private Long id;
    private String referenceNo;
    private Long userId;
    private String requestType;
    private String status;
    private String plateNumber;
    private String mvFileNumber;
    private String chassisNumber;
    private String engineNumber;
    private String voucherCode;
    private Long voucherRequestId;
    private Boolean hpgVerified;
    private String certificateNo;
    private String clientName;
    private LocalDateTime dateCreated;
}