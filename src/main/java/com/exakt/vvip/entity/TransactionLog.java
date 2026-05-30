package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "transaction_logs")
public class TransactionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String referenceNo;
    private String accountName;
    private String companyName;
    private String description;
    private String response;
    private String origin;
    private String status;
    private LocalDateTime dateCreated;

    @PrePersist
    protected void onCreate() {
        dateCreated = LocalDateTime.now();
    }
}