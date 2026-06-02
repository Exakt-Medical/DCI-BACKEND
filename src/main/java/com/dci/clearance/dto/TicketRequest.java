package com.dci.clearance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequest {

    private String referenceNumber;
    private String status;
    private String requestedBy;
    private String type;
    private String processedBy;
    private LocalDateTime dateUpdated;
    private LocalDateTime dateRequested;
    private String escalated;
    private String roleBased;

    private String mvFileNo;
    private String plateNo;
    private String engineNo;
    private String chassisNo;
    private String make;
    private String series;
    private String vehicleColor;
    private String vehicleTypeDenomination;
    private String yearModel;
    private String classification;
    private String name;
    private String address;

    private String certificateOfRegistration;
    private String crAttachment;
    private String plateCertification;
    private String actualPlate;
}