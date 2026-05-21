package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Column(name = "branch_shortname", length = 100)
    private String branchShortname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

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
