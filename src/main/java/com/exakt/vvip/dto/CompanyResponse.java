package com.exakt.vvip.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyResponse {
    private Long id;
    private String companyId;
    private String companyName;
    private String companyShortname;
    private String approvalStatus;
    private Boolean isactive;
    private String userstamp;
    private LocalDateTime timestamp;
}
