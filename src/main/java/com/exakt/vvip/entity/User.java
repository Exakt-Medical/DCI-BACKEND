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

    @Column(length = 20)
    private String mobile;

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

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "mfa_enabled")
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_verified")
    @Builder.Default
    private Boolean mfaVerified = false;

    @Column(name = "mfa_code", length = 50)
    @Builder.Default
    private String mfaCode = "000";

    @Column(name = "mfa_code_expiry", length = 50)
    @Builder.Default
    private String mfaCodeExpiry = "";

    @Column(name = "is_buy_voucher_allowed")
    @Builder.Default
    private Boolean isBuyVoucherAllowed = true;

    // @Column(name = "is_buy_voucher_allowed")
    // @Builder.Default
    // private Boolean allowedToBuyVoucher = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userstamp")
    private User userstamp;

    @Column(name = "date_created", length = 50, updatable = false)
    @Builder.Default
    private String dateCreated = new java.text.SimpleDateFormat("MMM. dd, yyyy hh:mm a").format(new java.util.Date());

    public enum UserRole {
        ADMIN, MANAGER, AGENT, SUBAGENT, VIEWER, SUPPORT
    }
}
