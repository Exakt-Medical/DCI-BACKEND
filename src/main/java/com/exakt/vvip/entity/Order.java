package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "company_code", length = 30, nullable = false)
    private String companyCode;

    @Column(name = "voucher_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal voucherFee;

    @Column(name = "voucher_count", nullable = false)
    private Integer voucherCount;

    @Column(name = "original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "processing_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "total_charged", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCharged;

    @Column(name = "merchant_reference_id", length = 100, nullable = false)
    private String merchantReferenceId;

    @Column(name = "tlpe_transaction_id", length = 100)
    private String tlpeTransactionId;

    @Column(name = "invoice_reference", length = 255)
    private String invoiceReference;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Column(name = "billeroo_confirmed", nullable = false)
    @Builder.Default
    private Boolean billerooConfirmed = false;

    @Column(name = "billeroo_confirmed_at")
    private LocalDateTime billerooConfirmedAt;

    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
