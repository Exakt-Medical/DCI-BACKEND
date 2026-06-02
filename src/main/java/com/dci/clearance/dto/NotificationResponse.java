package com.dci.clearance.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {
    private Long id;
    private Integer notifId;
    private String notifDetails;
    private String userstamp;
    private String userrole;
    private LocalDateTime timestamp;
}
