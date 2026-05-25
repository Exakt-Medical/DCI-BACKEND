package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "verification_requests")
public class VerificationRequest {

    public enum VerificationStatus { PENDING, VERIFIED, FAILED, ERROR }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_no", nullable = false, unique = true, length = 50)
    private String referenceNo;

    @Column(name = "mv_file_number",  length = 50)
    private String mvFileNumber;
    @Column(name = "plate_number",    length = 20)
    private String plateNumber;
    @Column(name = "chassis_number",  length = 50)
    private String chassisNumber;
    @Column(name = "engine_number",   length = 50)
    private String engineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;
    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @PrePersist  protected void onCreate() { dateCreated = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate()  { dateUpdated = LocalDateTime.now(); }
}