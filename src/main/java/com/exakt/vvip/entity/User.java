package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(length = 20)
    private String mobile;

    public enum UserRole {
        ADMIN, AGENT
    }
}