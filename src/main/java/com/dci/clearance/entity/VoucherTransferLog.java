package com.dci.clearance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "voucher_transfer_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherTransferLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    // Stored at transfer time — no join needed when loading history
    @Column(name = "to_agent_name", length = 255, nullable = false)
    private String toAgentName;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "voucher_code", length = 50, nullable = false)
    private String voucherCode;

    @Column(name = "transferred_at", nullable = false, updatable = false)
    private LocalDateTime transferredAt;

    @Column(name = "reference_number", length = 50, nullable = false)
    private String referenceNumber;

    @PrePersist
    protected void onCreate() {
        transferredAt = LocalDateTime.now(ZoneId.of("Asia/Manila"));
    }
}