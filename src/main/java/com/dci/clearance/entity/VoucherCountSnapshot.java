package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_count_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherCountSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "company_code", length = 30, nullable = false)
    private String companyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "billeroo_available", nullable = false)
    private Integer billerooAvailable;

    @Column(name = "billeroo_redeemed", nullable = false)
    private Integer billerooRedeemed;

    @Column(name = "billeroo_cancelled", nullable = false)
    private Integer billerooCancelled;

    @Column(name = "billeroo_total", nullable = false)
    private Integer billerooTotal;

    @Column(name = "local_available", nullable = false)
    private Integer localAvailable;

    @Column(name = "is_synced", nullable = false)
    private Boolean isSynced;

    @Column(name = "discrepancy")
    private Integer discrepancy;

    @Column(name = "trigger_type", length = 20, nullable = false)
    private String triggerType;

    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;
}
