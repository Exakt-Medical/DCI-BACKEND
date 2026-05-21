package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", unique = true, nullable = false, length = 50)
    private String companyId;

    @Column(name = "company_name", length = 500)
    private String companyName;

    @Column(name = "company_shortname", length = 100)
    private String companyShortname;

    @Column(name = "code", length = 100)
    @Builder.Default
    private String code = "";

    @Column(name = "date_created", length = 50)
    @Builder.Default
    private String dateCreated = "";

    @Column
    @Builder.Default
    private String name = "";

    @Column(name = "approval_status", length = 50)
    @Builder.Default
    private String approvalStatus = "PENDING";

    @Column(nullable = false)
    @Builder.Default
    private Boolean isactive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userstamp")
    private User userstamp;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}