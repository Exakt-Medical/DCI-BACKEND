package com.dci.clearance.entity;

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

    @Column(name = "reference_number", length = 100, nullable = false)
    private String referenceNumber;

    @Lob
    @Column(name = "cr_attachment", columnDefinition = "LONGBLOB")
    private byte[] crAttachment;

    @Lob
    @Column(name = "plate_certification_attachment", columnDefinition = "LONGBLOB")
    private byte[] plateCertificationAttachment;

    @Lob
    @Column(name = "actual_plate_attachment", columnDefinition = "LONGBLOB")
    private byte[] actualPlateAttachment;

    @Lob
    @Column(name = "attachmentcol", columnDefinition = "LONGBLOB")
    private byte[] attachmentCol;

    // Remove these fields since they don't exist in your database:
    // private String requestedBy;
    // private String status;
    // private LocalDateTime dateUpdated;
    // private LocalDateTime dateRequested;
}