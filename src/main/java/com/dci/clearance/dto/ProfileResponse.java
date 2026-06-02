package com.dci.clearance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private Long id;
    private String username;
    private String userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String mobile;
    private String role;
    private String status;
    private String companyName;
    private String branchName;
    private String employeeId;
    private String dateCreated;
}