package com.exakt.vvip.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String middleInitial;
    private String extName;
    private String email;
    private String mobile;
}