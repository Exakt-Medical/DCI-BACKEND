package com.dci.clearance.repository;

import com.dci.clearance.entity.VerificationOwnerDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationOwnerDetailsRepository extends JpaRepository<VerificationOwnerDetails, Long> {
    Optional<VerificationOwnerDetails> findByVerificationId(Long verificationId);
    java.util.List<VerificationOwnerDetails> findByVerificationIdIn(java.util.List<Long> verificationIds);
}
