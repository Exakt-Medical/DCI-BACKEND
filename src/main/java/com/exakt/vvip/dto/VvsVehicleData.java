package com.exakt.vvip.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VvsVehicleData {

    @JsonProperty("requestID")
    private String requestId;
    @JsonProperty("mvFileNo")
    private String mvFileNo;
    @JsonProperty("engineNumber")
    private String engineNo;
    @JsonProperty("chassisNumber")
    private String chassisNo;
    @JsonProperty("plateNumber")
    private String plateNo;
    @JsonProperty("make")
    private String make;
    @JsonProperty("mvType")
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

    // Nested owner object
    @JsonProperty("owner")
    private VvsOwner owner;

    // Nested address object
    @JsonProperty("address")
    private VvsAddress address;

    // ── Nested classes ────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VvsOwner {
        @JsonProperty("firstName")
        private String firstName;
        @JsonProperty("middelName")
        private String middleName;  // VVS typo
        @JsonProperty("lastName")
        private String lastName;
        @JsonProperty("organization")
        private String organization;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VvsAddress {
        @JsonProperty("houseBldgNo")
        private String houseBldgNo;
        @JsonProperty("streetName")
        private String streetName;
        @JsonProperty("barangay")
        private String barangay;
        @JsonProperty("municipality")
        private String municipality;
        @JsonProperty("province")
        private String province;
        @JsonProperty("region")
        private String region;
        @JsonProperty("zipCode")
        private String zipCode;
    }


    public String getOwnerFirstName() {
        return owner != null ? nvl(owner.getFirstName()) : "";
    }

    public String getOwnerMiddleName() {
        return owner != null ? nvl(owner.getMiddleName()) : "";
    }

    public String getOwnerLastName() {
        return owner != null ? nvl(owner.getLastName()) : "";
    }

    public String getFullOwnerName() {
        return String.join(" ",
                getOwnerFirstName(),
                getOwnerMiddleName(),
                getOwnerLastName()
        ).trim();
    }

    public String getFullAddress() {
        if (address == null) return "";
        return String.join(", ",
                nvl(address.getHouseBldgNo()),
                nvl(address.getStreetName()),
                nvl(address.getBarangay()),
                nvl(address.getMunicipality()),
                nvl(address.getProvince()),
                nvl(address.getRegion())
        );
    }

    private String nvl(String v) { return v != null ? v : ""; }
}