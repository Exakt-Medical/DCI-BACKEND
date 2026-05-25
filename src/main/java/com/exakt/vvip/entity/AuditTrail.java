package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_trail")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_trail_id")
    private Integer auditTrailId;

    @Column(name = "action_made", length = 100)
    private String actionMade;

    @Column(length = 500)
    private String details;

    @Column(length = 100)
    private String userstamp;

    @Column(length = 50)
    private String userrole;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
