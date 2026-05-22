package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BranchRequest {
    private String branchId;
    private String branchName;
    private String branchShortname;
    private Long companyId;
    private Boolean isactive;
}
