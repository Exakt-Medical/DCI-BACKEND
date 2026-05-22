package com.exakt.vvip.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditTrailResponse {
    private Long id;
    private Integer auditTrailId;
    private String actionMade;
    private String userstamp;
    private String userrole;
    private LocalDateTime timestamp;
}
