package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(length = 500)
    private String firstName;

    @Column(length = 500)
    private String lastName;

    @Column(length = 50)
    private String middleInitial;

    @Column(length = 50)
    private String extName;

    @Column(length = 500)
    private String email;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.AGENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(name = "active")
    @Builder.Default
    private Boolean isactive = true;

    @Builder.Default
    private Boolean isSubAgent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userstamp")
    private User userstamp;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum UserRole {
        ADMIN, MANAGER, AGENT
    }
}