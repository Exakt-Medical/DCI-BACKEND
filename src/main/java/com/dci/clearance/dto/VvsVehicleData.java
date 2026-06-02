package com.dci.clearance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Maps the raw VVS GetDetails JSON response.
 *
 * VVS response shape:
 * {
 *   "requestID": "71",
 *   "mvFileNo": "13242500000003A",
 *   "engineNumber": "ENG-PMVIC-2025-09030003",
 *   "chassisNumber": "CHA-PMVIC-2025-09030003",
 *   "plateNumber": "CEC2503",
 *   "make": "Honda",
 *   "series": "NA",
 *   "color": "NOT AVAILABLE",
 *   "yearModel": "2013",
 *   "classification": "PRIVATE",
 *   "bodyType": "SEDAN - 4-DOOR",
 *   "denomination": "LIGHT",
 *   "lastRegistrationDate": "10/01/2023",
 *   "owner": { "firstName": "...", "middelName": "...", "lastName": "...", "organization": "" },
 *   "address": { "houseBldgNo": "...", "streetName": "...", "barangay": "...",
 *                "municipality": "...", "province": "...", "region": "...", "zipCode": "..." }
 * }
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VvsVehicleData {

    // --- VVS request tracking ---
    @JsonProperty("requestID")
    private String requestId;

    // --- Vehicle identifiers ---
    @JsonProperty("mvFileNo")
    private String mvFileNo;

    @JsonProperty("plateNumber")
    private String plateNo;

    @JsonProperty("engineNumber")
    private String engineNo;

    @JsonProperty("chassisNumber")
    private String chassisNo;

    // --- Vehicle details ---
    @JsonProperty("make")
    private String make;

    @JsonProperty("series")
    private String series;

    @JsonProperty("color")
    private String color;

    @JsonProperty("yearModel")
    private String yearModel;

    @JsonProperty("classification")
    private String classification;

    @JsonProperty("bodyType")
    private String bodyType;

    @JsonProperty("denomination")
    private String denomination;

    @JsonProperty("lastRegistrationDate")
    private String lastRegistrationDate;

    // --- Owner (nested object flattened via @JsonProperty on setter) ---
    @JsonProperty("owner")
    private void unpackOwner(java.util.Map<String, String> owner) {
        if (owner == null) return;
        this.ownerFirstName  = owner.get("firstName");
        this.ownerMiddleName = owner.get("middelName");   // VVS typo: "middelName"
        this.ownerLastName   = owner.get("lastName");
        this.ownerOrganization = owner.get("organization");
    }

    private String ownerFirstName;
    private String ownerMiddleName;
    private String ownerLastName;
    private String ownerOrganization;

    // --- Address (nested object flattened) ---
    @JsonProperty("address")
    private void unpackAddress(java.util.Map<String, String> address) {
        if (address == null) return;
        this.addressHouseBldgNo  = address.get("houseBldgNo");
        this.addressStreetName   = address.get("streetName");
        this.addressBarangay     = address.get("barangay");
        this.addressMunicipality = address.get("municipality");
        this.addressProvince     = address.get("province");
        this.addressRegion       = address.get("region");
        this.addressZipCode      = address.get("zipCode");
    }

    private String addressHouseBldgNo;
    private String addressStreetName;
    private String addressBarangay;
    private String addressMunicipality;
    private String addressProvince;
    private String addressRegion;
    private String addressZipCode;

    // --- Convenience: full owner name for display ---
    public String getFullOwnerName() {
        return String.join(" ",
                blankToEmpty(ownerFirstName),
                blankToEmpty(ownerMiddleName),
                blankToEmpty(ownerLastName)
        ).trim();
    }

    private static String blankToEmpty(String s) {
        return (s == null || s.isBlank() || s.equalsIgnoreCase("NPLA")) ? "" : s.trim();
    }
}