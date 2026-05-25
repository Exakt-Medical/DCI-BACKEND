package com.exakt.vvip.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VvsLookupResponse {
    private String mvFileNumber;
    private String plateNumber;
    private String engineNumber;
    private String chassisNumber;
    private String make;
    private String series;
    private String color;
    private String yearModel;
    private String bodyType;
    private String ownerFullName;
    private boolean found;
}
