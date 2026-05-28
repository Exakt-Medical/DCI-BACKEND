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
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(length = 50)
    private String status;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @Column(name = "date_requested")
    private LocalDateTime dateRequested;

    @Lob
    @Column(name = "cr_attachment")
    private byte[] crAttachment;

    @Lob
    @Column(name = "plate_certification_attachment")
    private byte[] plateCertificationAttachment;

    @Lob
    @Column(name = "actual_plate_attachment")
    private byte[] actualPlateAttachment;
}