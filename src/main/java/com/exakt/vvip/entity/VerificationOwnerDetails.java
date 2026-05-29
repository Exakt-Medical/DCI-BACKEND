package com.exakt.vvip.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter @Setter
@Entity
@Table(name = "verification_owner_details")
public class VerificationOwnerDetails {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verification_id", nullable = false, unique = true)
    private Long verificationId;

    @Column(name = "first_name",    length = 100) private String firstName;
    @Column(name = "middle_name",   length = 100) private String middleName;
    @Column(name = "last_name",     length = 100) private String lastName;
    @Column(name = "organization",  length = 200) private String organization;
    @Column(name = "house_bldg_no", length = 100) private String houseBldgNo;
    @Column(name = "street_name",   length = 200) private String streetName;
    @Column(name = "barangay",      length = 100) private String barangay;
    @Column(name = "municipality",  length = 100) private String municipality;
    @Column(name = "province",      length = 100) private String province;
    @Column(name = "region",        length = 100) private String region;
    @Column(name = "zip_code",      length = 10)  private String zipCode;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @PrePersist protected void onCreate() { dateCreated = LocalDateTime.now(); }
}
