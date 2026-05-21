package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String agent;

    @Column(length = 150)
    private String company;

    @Column(length = 200)
    private String assuredName;

    @Column(unique = true, nullable = false, length = 30)
    private String authNo;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Column(length = 50)
    private String mvFileNumber;

    @Column(length = 20)
    private String plateNumber;

    @Column(length = 100)
    private String policyNumber;

    @Column(length = 50)
    private String voucherCode;

    @Column(length = 100)
    private String premiumType;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}