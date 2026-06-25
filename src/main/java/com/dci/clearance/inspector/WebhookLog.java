package com.dci.clearance.inspector;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
