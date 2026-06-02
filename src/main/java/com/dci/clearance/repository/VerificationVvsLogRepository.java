package com.dci.clearance.repository;

import com.dci.clearance.entity.VerificationVvsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationVvsLogRepository extends JpaRepository<VerificationVvsLog, Long> {
    Optional<VerificationVvsLog> findByVerificationId(Long verificationId);
}
