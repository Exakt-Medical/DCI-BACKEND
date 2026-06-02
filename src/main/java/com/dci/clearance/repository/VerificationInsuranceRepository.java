package com.dci.clearance.repository;

import com.dci.clearance.entity.VerificationInsurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationInsuranceRepository extends JpaRepository<VerificationInsurance, Long> {
    Optional<VerificationInsurance> findByVerificationId(Long verificationId);
}
