package com.dci.clearance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentRequest {

    private String referenceNumber;

    private String requestedBy;

    private byte[] crAttachment;

    private byte[] plateCertificationAttachment;

    private byte[] actualPlateAttachment;
}