package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account", nullable = false)
    private String account;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "ref_no", nullable = false)
    private String refNo;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "origin")
    private String origin;

    @Column(name = "status")
    private String status;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;
}