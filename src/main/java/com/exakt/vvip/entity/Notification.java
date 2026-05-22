package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notif_id")
    private Integer notifId;

    @Column(name = "notif_details", length = 500)
    private String notifDetails;

    @Column(length = 100)
    private String userstamp;

    @Column(length = 50)
    private String userrole;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
