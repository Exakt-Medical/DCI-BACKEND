package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(length = 100)
    private String coverage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String description;

    @Builder.Default
    private Integer validityDays = 365;

    @Column(nullable = false, length = 100)
    private String insuranceCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}