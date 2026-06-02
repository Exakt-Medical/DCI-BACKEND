package com.dci.clearance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class NotificationRequest {
    private Integer notifId;
    private String notifDetails;
    private String userstamp;
    private String userrole;
}
