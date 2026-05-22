package com.exakt.vvip.dto;

import lombok.*;
import java.time.LocalDateTime;

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
    private String role;
    private Long branchId;
    private String branchName;
    private Long managerId;
    private String managerName;
    private Boolean isactive;
    private Boolean isSubAgent;
    private String userstamp;
    private LocalDateTime timestamp;
}
