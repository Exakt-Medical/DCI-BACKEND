package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherTransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_code", length = 50, nullable = false, unique = true)
    private String voucherCode;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "company_code", length = 30, nullable = false)
    private String companyCode;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "tlpe_transaction_id", length = 100, nullable = false)
    private String tlpeTransactionId;

    @Column(name = "merchant_reference", length = 100, nullable = false)
    private String merchantReference;

    @Column(name = "payment_reference", length = 255, nullable = false)
    private String paymentReference;

    @Column(name = "original_user_id", nullable = false)
    private Long originalUserId;

    @Column(name = "current_user_id", nullable = false)
    private Long currentUserId;

    @Column(length = 20, nullable = false)
    private String status; // AVAILABLE, TRANSFERRED, REDEEMED, EXPIRED

    @Column(name = "voucher_reference", length = 255)
    private String voucherReference;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Manila"));
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Manila"));
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Manila"));
    }
}