package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BranchRequest {
    private String branchId;
    private String branchName;
    private Long companyId;
    private String companyCode;
    private String status;
}
