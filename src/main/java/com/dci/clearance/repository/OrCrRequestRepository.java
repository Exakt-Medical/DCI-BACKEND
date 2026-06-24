package com.dci.clearance.repository;

import com.dci.clearance.entity.OrCrRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrCrRequestRepository extends JpaRepository<OrCrRequest, Long> {
    Optional<OrCrRequest> findByCertificateRequestId(Long certificateRequestId);
}
