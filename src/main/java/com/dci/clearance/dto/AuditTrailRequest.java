package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuditTrailRequest {
    private Integer auditTrailId;
    private String actionMade;
    private String details;
    private String userstamp;
    private String userrole;
}
