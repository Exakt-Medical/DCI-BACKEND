package com.exakt.vvip.repository;

import com.exakt.vvip.entity.VerificationOwnerDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationOwnerDetailsRepository extends JpaRepository<VerificationOwnerDetails, Long> {
    Optional<VerificationOwnerDetails> findByVerificationId(Long verificationId);
}
