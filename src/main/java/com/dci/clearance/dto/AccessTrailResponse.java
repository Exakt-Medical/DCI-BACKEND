package com.dci.clearance.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTrailResponse {
    private Long id;
    private String username;
    private LocalDateTime timestamp;
}
