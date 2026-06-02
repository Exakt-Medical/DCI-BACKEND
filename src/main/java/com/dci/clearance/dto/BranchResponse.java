package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BranchResponse {
    private Long id;
    private String branchId;
    private String branchName;
    private Long companyId;
    private String companyCode;
    private String companyName;
    private String companyProvider;
    private String status;
    private String userstamp;
    private String dateCreated;
}
