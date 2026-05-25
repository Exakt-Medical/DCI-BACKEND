package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "verification_vvs_logs")
public class VerificationVvsLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verification_id", nullable = false, unique = true)
    private Long verificationId;

    @Column(name = "vvs_request_id",  length = 50)
    private String vvsRequestId;
    @Column(name = "vvs_token",       columnDefinition = "TEXT")
    private String vvsToken;

    @Column(name = "vvs_mv_plate_response",       columnDefinition = "MEDIUMTEXT")
    private String vvsMvPlateResponse;

    @Column(name = "vvs_engine_chassis_response", columnDefinition = "MEDIUMTEXT")
    private String vvsEngineChassisResponse;

    @Column(name = "vvs_confirm_response",        length = 500)
    private String vvsConfirmResponse;

    @Column(name = "matched_fields")
    private Integer matchedFields = 0;
    @Column(name = "matched_field_names", length = 200)
    private String matchedFieldNames;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @PrePersist protected void onCreate() { dateCreated = LocalDateTime.now(); }
}
