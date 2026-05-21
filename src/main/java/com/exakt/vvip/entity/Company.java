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

    @Column(unique = true, nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    @Builder.Default
    private String provider = "LTO";

    @Column(nullable = false, length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CompanyStatus status = CompanyStatus.PENDING;

    @Column(length = 100)
    private String branch;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dateCreated = LocalDateTime.now();

    public enum CompanyStatus {
        PENDING, ACTIVE, INACTIVE, DECLINED, DEACTIVATED
    }
}