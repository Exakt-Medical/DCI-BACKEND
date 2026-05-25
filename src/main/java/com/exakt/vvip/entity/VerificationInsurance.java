package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "verification_insurance")
public class VerificationInsurance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verification_id", nullable = false, unique = true)
    private Long verificationId;

    @Column(name = "insurance_code",  length = 100)
    private String insuranceCode;
    @Column(name = "policy_number",   length = 50)
    private String policyNumber;
    @Column(name = "premium_type",    length = 100)
    private String premiumType;

    @Column(name = "prescribed_premium_fee", precision = 10, scale = 2)
    private BigDecimal prescribedPremiumFee;

    @Column(name = "dst",            precision = 10, scale = 2)
    private BigDecimal dst;
    @Column(name = "vat",            precision = 10, scale = 2)
    private BigDecimal vat;
    @Column(name = "lgt",            precision = 10, scale = 2)
    private BigDecimal lgt;
    @Column(name = "validation_fee", precision = 10, scale = 2)
    private BigDecimal validationFee;
    @Column(name = "total_amount",   precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "voucher_code",   length = 50)
    private String voucherCode;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @PrePersist protected void onCreate() { dateCreated = LocalDateTime.now(); }
}
