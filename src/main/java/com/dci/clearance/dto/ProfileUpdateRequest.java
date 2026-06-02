package com.dci.clearance.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
}