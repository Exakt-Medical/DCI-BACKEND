package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String mvFileNumber;

    @Column(length = 20)
    private String plateNumber;

    @Column(length = 50)
    private String engineNumber;

    @Column(length = 50)
    private String chassisNumber;

    @Column(length = 50)
    private String make;

    @Column(length = 100)
    private String series;

    @Column(length = 30)
    private String color;

    @Column(length = 10)
    private String yearModel;

    @Column(length = 30)
    private String classification;

    @Column(length = 30)
    private String bodyType;

    @Column(length = 50)
    private String vehicleCategory;

    @Column(length = 50)
    private String vehicleType;

    @Column(length = 50)
    private String lastRegistrationDate;

    @Column(length = 100)
    private String ownerFirstName;

    @Column(length = 100)
    private String ownerLastName;

    @Column(length = 100)
    private String ownerMiddleName;

    @Column(length = 255)
    private String ownerAddress;

    @Column(length = 30)
    private String ownerContactNo;

    @Column(length = 100)
    private String ownerEmail;

    @Column(length = 30)
    private String ownerTin;
}