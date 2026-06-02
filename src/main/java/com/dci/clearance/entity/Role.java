package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", unique = true, nullable = false, length = 50)
    private String roleId;

    @Column(name = "role_name", length = 100)
    private String roleName;
}
