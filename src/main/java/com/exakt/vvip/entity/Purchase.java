package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String policyNumber;

    @Column(unique = true, nullable = false, length = 50)
    private String voucherCode;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal premium;

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.ACTIVE;

    @Column
    private LocalDateTime redeemedOn;

    @Column(nullable = false, length = 100)
    private String insuranceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchased_by")
    private User purchasedBy;

    public enum PurchaseStatus {
        ACTIVE, REDEEMED, EXPIRED
    }
}