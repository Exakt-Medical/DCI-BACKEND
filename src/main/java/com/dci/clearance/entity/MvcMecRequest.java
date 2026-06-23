package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "mvcc_mec_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MvcMecRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_request_id", nullable = false)
    private CertificateRequest certificateRequest;

    @Column(name = "mvc_no", length = 100)
    private String mvcNo;

    @Column(name = "mvc_issue_date", length = 50)
    private String mvcIssueDate;

    @Column(name = "mvc_status", length = 50)
    private String mvcStatus;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "plate_number", length = 50)
    private String plateNumber;

    @Column(name = "mv_file_number", length = 50)
    private String mvFileNumber;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "engine_no_stencilled", length = 100)
    private String engineNoStencilled;

    @Column(name = "chassis_no_stencilled", length = 100)
    private String chassisNoStencilled;

    @Column(name = "hpg_technician", length = 150)
    private String hpgTechnician;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;
}
