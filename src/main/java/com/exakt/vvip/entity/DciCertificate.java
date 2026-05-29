package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "dci_certificates")
public class DciCertificate {

    public enum CertificateStatus { ACTIVE, REVOKED, EXPIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_no", nullable = false, unique = true, length = 50)
    private String certificateNo;

    @Column(name = "verification_id", nullable = false, unique = true)
    private Long verificationId;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CertificateStatus status = CertificateStatus.ACTIVE;

    @Column(name = "issued_by")
    private Long issuedBy;

    @Column(name = "issuer_name",  length = 200) private String issuerName;
    @Column(name = "company_name", length = 200) private String companyName;
    @Column(name = "premium_type", length = 100) private String premiumType;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @PrePersist protected void onCreate() { dateCreated = LocalDateTime.now(); }
}