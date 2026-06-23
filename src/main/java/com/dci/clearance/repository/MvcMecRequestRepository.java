package com.dci.clearance.repository;

import com.dci.clearance.entity.MvcMecRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MvcMecRequestRepository extends JpaRepository<MvcMecRequest, Long> {
    Optional<MvcMecRequest> findByCertificateRequestId(Long certificateRequestId);
}
