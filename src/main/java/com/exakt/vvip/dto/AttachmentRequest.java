package com.exakt.vvip.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentRequest {

    private String referenceNumber;
    private String crAttachment;
    private String plateCertificationAttachment;
    private String actualPlateAttachment;
}