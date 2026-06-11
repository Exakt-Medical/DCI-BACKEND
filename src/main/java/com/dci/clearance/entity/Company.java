package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", length = 500)
    private String companyName;

    @Column(length = 255)
    private String email;

    @Column(name = "code", length = 100, unique = true)
    @Builder.Default
    private String code = "";

    @Column(length = 200)
    private String provider;

    @Column(length = 1000)
    private String address;

    @Column(name = "date_created", length = 50, updatable = false)
    @Builder.Default
    private String dateCreated = new java.text.SimpleDateFormat("MMM. dd, yyyy hh:mm a").format(new java.util.Date());

    @Column(name = "approval_status", length = 50)
    @Builder.Default
    private String approvalStatus = "PENDING";

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "available_vouchers", nullable = false)
    @Builder.Default
    private Integer availableVouchers = 0;

    @Column(name = "billeroo_sync_status", length = 20, nullable = false)
    @Builder.Default
    private String billerooSyncStatus = "PENDING";

    @Column(name = "userstamp", length = 50)
    private String userstamp;
}
