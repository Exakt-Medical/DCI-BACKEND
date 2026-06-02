package com.dci.clearance.entity;

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

    @Column(length = 500)
    private String firstName;

    @Column(length = 500)
    private String lastName;

    @Column(length = 500)
    private String email;

    @Column(length = 20)
    private String mobile;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.CITIZEN;

    @Column(name = "company_code", length = 100)
    private String companyCode;

    @Column(name = "branch_ref", length = 50)
    private String branchRef;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "userstamp", length = 50)
    private String userstamp;

    @Column(name = "date_created", length = 50, updatable = false)
    @Builder.Default
    private String dateCreated = new java.text.SimpleDateFormat("MMM. dd, yyyy hh:mm a").format(new java.util.Date());

    public enum UserRole {
        CITIZEN, AGENT_FIXER, HPG, LTO, ADMIN
    }
}