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

    @Column(name = "make_brand", length = 100)
    private String makeBrand;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "classification", length = 50)
    private String classification;

    @Column(name = "series", length = 100)
    private String series;

    @Column(name = "year_model", length = 50)
    private String yearModel;

    @Column(name = "owner_name", length = 255)
    private String ownerName;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;
}
