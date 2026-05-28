package com.exakt.vvip.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VehicleVerificationResponse {

    // --- Transaction info ---
    private String        referenceNo;
    private Long          verificationId;
    private String        verificationStatus;
    private String        failureReason;
    private String        certificateNo;
    private LocalDateTime processedAt;

    // --- Vehicle details (matches frontend field mapping) ---
    private String mvFileNo;
    private String plateNumber;
    private String engineNumber;
    private String chassisNumber;
    private String make;
    private String series;
    private String color;
    private String yearModel;
    private String classification;
    private String bodyType;
    private String denomination;          // Vehicle Category / Vehicle Type
    private String lastRegistrationDate;

    // --- Owner details ---
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerMiddleName;

    // --- Address (combined into one display string) ---
    private String ownerAddress;

    // -------------------------------------------------------------------------
    // STEP 1 success: VVS returned a vehicle record
    // -------------------------------------------------------------------------
    public static VehicleVerificationResponse verified(String referenceNo,
                                                       Long verificationId,
                                                       VvsVehicleData v) {
        VehicleVerificationResponse r = new VehicleVerificationResponse();
        r.referenceNo        = referenceNo;
        r.verificationId     = verificationId;
        r.verificationStatus = "VERIFIED";
        r.processedAt        = LocalDateTime.now();

        if (v != null) {
            r.mvFileNo              = v.getMvFileNo();
            r.plateNumber           = v.getPlateNo();
            r.engineNumber          = v.getEngineNo();
            r.chassisNumber         = v.getChassisNo();
            r.make                  = v.getMake();
            r.series                = v.getSeries();
            r.color                 = v.getColor();
            r.yearModel             = v.getYearModel();
            r.classification        = v.getClassification();
            r.bodyType              = v.getBodyType();
            r.denomination          = v.getDenomination();
            r.lastRegistrationDate  = v.getLastRegistrationDate();
            r.ownerFirstName        = v.getOwnerFirstName();
            r.ownerLastName         = v.getOwnerLastName();
            r.ownerMiddleName       = v.getOwnerMiddleName();
            r.ownerAddress          = buildAddress(v);
        }

        return r;
    }

    // -------------------------------------------------------------------------
    // STEP 1 failure: VVS returned no matching record
    // -------------------------------------------------------------------------
    public static VehicleVerificationResponse failed(String referenceNo, String reason) {
        VehicleVerificationResponse r = new VehicleVerificationResponse();
        r.referenceNo        = referenceNo;
        r.verificationStatus = "FAILED";
        r.failureReason      = reason;
        r.processedAt        = LocalDateTime.now();
        return r;
    }

    // -------------------------------------------------------------------------
    // STEP 2 success: ConfirmRequest succeeded, certificate issued
    // -------------------------------------------------------------------------
    public static VehicleVerificationResponse confirmed(String referenceNo, String certNo) {
        VehicleVerificationResponse r = new VehicleVerificationResponse();
        r.referenceNo        = referenceNo;
        r.verificationStatus = "COMPLETED";
        r.certificateNo      = certNo;
        r.processedAt        = LocalDateTime.now();
        return r;
    }

    // -------------------------------------------------------------------------
    // Any step: API or internal error
    // -------------------------------------------------------------------------
    public static VehicleVerificationResponse error(String referenceNo, String reason) {
        VehicleVerificationResponse r = new VehicleVerificationResponse();
        r.referenceNo        = referenceNo;
        r.verificationStatus = "ERROR";
        r.failureReason      = reason;
        r.processedAt        = LocalDateTime.now();
        return r;
    }

    // -------------------------------------------------------------------------
    // Combines address fields into one readable string
    // e.g. "3 A SAN ROQUE VILLAGE, BAGUIO CITY, BENGUET, CAR 2600"
    // -------------------------------------------------------------------------
    private static String buildAddress(VvsVehicleData v) {
        if (v == null) return null;
        return String.join(", ",
                nonBlank(v.getAddressHouseBldgNo() + " " + v.getAddressStreetName()),
                nonBlank(v.getAddressMunicipality()),
                nonBlank(v.getAddressProvince()),
                nonBlank(v.getAddressRegion()),
                nonBlank(v.getAddressZipCode())
        ).replaceAll(",\\s*,", ",").strip();
    }

    private static String nonBlank(String s) {
        return (s == null || s.isBlank()) ? "" : s.trim();
    }
}