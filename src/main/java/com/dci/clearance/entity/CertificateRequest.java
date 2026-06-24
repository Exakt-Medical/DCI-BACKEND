package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CertificateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "verification_id")
    private Long verificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "current_step")
    private Integer currentStep;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "certificate_no", length = 100)
    private String certificateNo;

    @Column(name = "voucher_code", length = 100)
    private String voucherCode;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @PrePersist
    protected void onCreate() { dateCreated = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { dateUpdated = LocalDateTime.now(); }
}
