package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "cr_attachment", length = 255)
    private String crAttachment;

    @Column(name = "plate_certification_attachment", length = 255)
    private String plateCertificationAttachment;

    @Column(name = "actual_plate_attachment", length = 255)
    private String actualPlateAttachment;
}