package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", unique = true, nullable = false, length = 50)
    private String branchId;

    @Column(name = "branch_name", length = 500)
    private String branchName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userstamp")
    private User userstamp;

    @Column(name = "date_created", length = 50, updatable = false)
    @Builder.Default
    private String dateCreated = new java.text.SimpleDateFormat("MMM. dd, yyyy hh:mm a").format(new java.util.Date());
}
