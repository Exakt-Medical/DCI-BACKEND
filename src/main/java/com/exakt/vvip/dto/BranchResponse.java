package com.exakt.vvip.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BranchResponse {
    private Long id;
    private String branchId;
    private String branchName;
    private String branchShortname;
    private Long companyId;
    private String companyName;
    private Boolean isactive;
    private String userstamp;
    private LocalDateTime timestamp;
}
