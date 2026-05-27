package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BranchResponse {
    private Long id;
    private String branchId;
    private String branchName;
    private Long companyId;
    private String companyName;
    private String status;
    private String userstamp;
    private String dateCreated;
}
