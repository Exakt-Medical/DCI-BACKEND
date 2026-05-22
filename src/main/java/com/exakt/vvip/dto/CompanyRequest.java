package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CompanyRequest {
    private String companyId;
    private String companyName;
    private String companyShortname;
    private String approvalStatus;
    private Boolean isactive;
}
