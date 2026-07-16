package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_trail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "action", length = 20)
    private String action;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
