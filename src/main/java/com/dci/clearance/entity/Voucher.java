package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers", indexes = {
    @Index(name = "idx_vouchers_company_code", columnList = "company_code"),
    @Index(name = "idx_vouchers_current_user", columnList = "current_user_id"),
    @Index(name = "idx_vouchers_status", columnList = "status"),
    @Index(name = "idx_vouchers_tlpe_tx", columnList = "tlpe_transaction_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_code", length = 50, nullable = false, unique = true)
    private String voucherCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "company_code", length = 30, nullable = false)
    private String companyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "tlpe_transaction_id", length = 100, nullable = false)
    private String tlpeTransactionId;

    @Column(name = "merchant_reference", length = 100, nullable = false)
    private String merchantReference;

    @Column(name = "payment_reference", length = 255, nullable = false)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_user_id", nullable = false)
    private User originalUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_user_id", nullable = false)
    private User currentUser;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "voucher_reference", length = 255)
    private String voucherReference;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
