package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(length = 50)
    private String status;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(length = 100)
    private String type;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @Column(name = "date_requested")
    private LocalDateTime dateRequested;

    @Column(length = 50)
    private String escalated;

    @Column(name = "role_based", length = 100)
    private String roleBased;

    @Column(name = "mv_file_no", length = 100)
    private String mvFileNo;

    @Column(name = "plate_no", length = 50)
    private String plateNo;

    @Column(name = "engine_no", length = 100)
    private String engineNo;

    @Column(name = "chassis_no", length = 100)
    private String chassisNo;

    @Column(length = 100)
    private String make;

    @Column(length = 100)
    private String series;

    @Column(name = "vehicle_color", length = 50)
    private String vehicleColor;

    @Column(name = "vehicle_type_denomination", length = 150)
    private String vehicleTypeDenomination;

    @Column(name = "year_model", length = 20)
    private String yearModel;

    @Column(length = 100)
    private String classification;

    @Column(length = 150)
    private String name;

    @Column(length = 300)
    private String address;

//    @Column(name = "certificate_of_registration", length = 255)
//    private String certificateOfRegistration;

    @Lob
    @Column(name = "cr_attachment", columnDefinition = "TEXT")
    private String crAttachment;

 

    @Column(name = "plate_certification_attachment", length = 255)
    private String plateCertification;

    @Column(name = "actual_plate_attachment", length = 255)
    private String actualPlate;
}