package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String username;
    private String userId;
    private String firstName;
    private String lastName;
    private String middleInitial;
    private String extName;
    private String email;
    private String mobile;
    private String role;
    private Long branchId;
    private String branchName;
    private String branchCompanyName;
    private Long managerId;
    private String managerName;
    private String managerBranchName;
    private String managerBranchCompanyName;
    private String status;
    private Boolean mfaEnabled;
    private Boolean mfaVerified;
    private String mfaCode;
    private String mfaCodeExpiry;
    private Boolean isBuyVoucherAllowed;
    private String userstamp;
    private String dateCreated;
}
