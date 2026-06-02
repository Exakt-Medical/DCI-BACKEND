package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "insurance_fees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InsuranceFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String insuranceCode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prescribedPremiumFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dst;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal vat;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lgt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal validationFee;
}