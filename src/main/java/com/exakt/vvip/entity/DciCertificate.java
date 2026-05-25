package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "dci_certificates")
public class DciCertificate {

    public enum CertificateStatus { ACTIVE, REVOKED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_no", nullable = false, unique = true, length = 50)
    private String certificateNo;

    @Column(name = "verification_id", nullable = false, unique = true)
    private Long verificationId;

    @Column(name = "mv_file_number",  length = 50)
    private String mvFileNumber;
    @Column(name = "plate_number",    length = 20)
    private String plateNumber;
    @Column(name = "chassis_number",  length = 50)
    private String chassisNumber;
    @Column(name = "engine_number",   length = 50)
    private String engineNumber;
    @Column(name = "owner_name",      length = 200)
    private String ownerName;

    @Column(name = "issued_date",   nullable = false)
    private LocalDate issuedDate;
    @Column(name = "expiry_date",   nullable = false)
    private LocalDate expiryDate;
    @Column(name = "pdf_file_path", length = 500)
    private String pdfFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CertificateStatus status = CertificateStatus.ACTIVE;

    @Column(name = "issued_by")
    private Long issuedBy;
    @Column(name = "company_code", length = 100)
    private String companyCode;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @PrePersist
    protected void onCreate() { this.dateCreated = LocalDateTime.now(); }
}