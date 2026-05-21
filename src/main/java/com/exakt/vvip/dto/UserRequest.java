package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserRequest {
    private String username;
    private String password;
    private String userId;
    private String firstName;
    private String lastName;
    private String middleInitial;
    private String extName;
    private String email;
    private String role;
    private Long branchId;
    private Long managerId;
    private Boolean isactive;
    private Boolean isSubAgent;
}
