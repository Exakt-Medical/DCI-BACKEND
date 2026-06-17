package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "verification_vehicle_details")
public class VerificationVehicleDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verification_id", nullable = false, unique = true)
    private Long verificationId;

    @Column(name = "make", length = 100)
    private String make;
    @Column(name = "series", length = 100)
    private String series;
    @Column(name = "color", length = 50)
    private String color;
    @Column(name = "year_model", length = 10)
    private String yearModel;
    @Column(name = "classification", length = 100)
    private String classification;
    @Column(name = "body_type", length = 100)
    private String bodyType;
    @Column(name = "denomination", length = 100)
    private String denomination;
    @Column(name = "last_registration_date", length = 50)
    private String lastRegistrationDate;

    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @PrePersist protected void onCreate() { dateCreated = LocalDateTime.now(); }
}
