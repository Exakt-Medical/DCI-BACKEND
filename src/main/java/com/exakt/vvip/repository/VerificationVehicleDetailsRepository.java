package com.exakt.vvip.repository;

import com.exakt.vvip.entity.VerificationVehicleDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationVehicleDetailsRepository extends JpaRepository<VerificationVehicleDetails, Long> {
    Optional<VerificationVehicleDetails> findByVerificationId(Long verificationId);
}
