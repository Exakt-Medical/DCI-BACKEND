package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "or_cr_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrCrRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_request_id", nullable = false)
    private CertificateRequest certificateRequest;

    @Column(name = "vehicle_option", length = 20)
    private String vehicleOption;

    @Column(name = "plate_number", length = 50)
    private String plateNumber;

    @Column(name = "mv_file_number", length = 50)
    private String mvFileNumber;

    @Column(name = "engine_number", length = 100)
    private String engineNumber;

    @Column(name = "chassis_number", length = 100)
    private String chassisNumber;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;
}
