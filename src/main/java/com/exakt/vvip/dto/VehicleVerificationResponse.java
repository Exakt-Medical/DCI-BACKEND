package com.exakt.vvip.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class VehicleVerificationResponse {
    private String referenceNo;
    private String verificationStatus;
    private int matchedFields;
    private String matchedFieldNames;
    private String failureReason;
    private String certificateNo;
    private LocalDateTime processedAt;

    public static VehicleVerificationResponse verified(String referenceNo, int matched,
                                                       String matchedNames, String certNo) {
        VehicleVerificationResponse r = new VehicleVerificationResponse();
        r.referenceNo        = referenceNo;
        r.verificationStatus = "VERIFIED";
        r.matchedFields      = matched;
        r.matchedFieldNames  = matchedNames;
        r.certificateNo      = certNo;
        r.processedAt        = LocalDateTime.now();
        return r;
    }

    public static VehicleVerificationResponse failed(String referenceNo, int matched,
                                                     String matchedNames, String reason) {
        VehicleVerificationResponse r = new VehicleVerificationResponse();
        r.referenceNo        = referenceNo;
        r.verificationStatus = "FAILED";
        r.matchedFields      = matched;
        r.matchedFieldNames  = matchedNames;
        r.failureReason      = reason;
        r.processedAt        = LocalDateTime.now();
        return r;
    }

    public static VehicleVerificationResponse error(String referenceNo, String reason) {
        VehicleVerificationResponse r = new VehicleVerificationResponse();
        r.referenceNo        = referenceNo;
        r.verificationStatus = "ERROR";
        r.failureReason      = reason;
        r.processedAt        = LocalDateTime.now();
        return r;
    }
}