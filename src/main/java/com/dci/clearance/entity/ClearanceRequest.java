package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clearance_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClearanceRequest {

    public enum RequestType {
        VOUCHER_REQUEST, CLEARANCE_REQUEST
    }

    public enum ClearanceStatus {
        DRAFT, ORCR_UPLOADED, PAYMENT_PENDING, PAYMENT_COMPLETED,
        VOUCHER_ISSUED, HPG_VERIFICATION, MVC_MEC_UPLOADED, CERTIFICATE_ISSUED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_no", nullable = false, unique = true, length = 50)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", length = 20)
    @Builder.Default
    private RequestType requestType = RequestType.VOUCHER_REQUEST;

    @Column(name = "voucher_request_id")
    private Long voucherRequestId;

    @Column(name = "or_cr_image", columnDefinition = "LONGBLOB")
    private byte[] orCrImage;

    @Column(name = "or_cr_ocr_data", columnDefinition = "TEXT")
    private String orCrOcrData;

    @Column(name = "mv_file_number", length = 50)
    private String mvFileNumber;

    @Column(name = "plate_number", length = 20)
    private String plateNumber;

    @Column(name = "chassis_number", length = 50)
    private String chassisNumber;

    @Column(name = "engine_number", length = 50)
    private String engineNumber;

    @Column(name = "vehicle_make", length = 100)
    private String vehicleMake;

    @Column(name = "vehicle_series", length = 100)
    private String vehicleSeries;

    @Column(name = "vehicle_year_model", length = 20)
    private String vehicleYearModel;

    @Column(name = "vehicle_color", length = 50)
    private String vehicleColor;

    @Column(name = "owner_name", length = 200)
    private String ownerName;

    @Column(name = "owner_address", length = 500)
    private String ownerAddress;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "mvc_mec_image", columnDefinition = "LONGBLOB")
    private byte[] mvcMecImage;

    @Column(name = "mvc_mec_ocr_data", columnDefinition = "TEXT")
    private String mvcMecOcrData;

    @Column(name = "hpg_verified")
    @Builder.Default
    private Boolean hpgVerified = false;

    @Column(name = "hpg_verified_by")
    private Long hpgVerifiedBy;

    @Column(name = "hpg_verified_at")
    private LocalDateTime hpgVerifiedAt;

    @Column(name = "certificate_no", length = 50)
    private String certificateNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ClearanceStatus status = ClearanceStatus.DRAFT;

    @Column(name = "agent_fixer_id")
    private Long agentFixerId;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @PrePersist
    protected void onCreate() { dateCreated = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { dateUpdated = LocalDateTime.now(); }
}