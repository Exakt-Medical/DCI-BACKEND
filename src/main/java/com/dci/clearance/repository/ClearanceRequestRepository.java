package com.dci.clearance.repository;

import com.dci.clearance.entity.ClearanceRequest;
import com.dci.clearance.entity.ClearanceRequest.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClearanceRequestRepository extends JpaRepository<ClearanceRequest, Long> {
    List<ClearanceRequest> findByUserIdOrderByDateCreatedDesc(Long userId);
    List<ClearanceRequest> findByUserIdAndRequestTypeOrderByDateCreatedDesc(Long userId, RequestType requestType);
    List<ClearanceRequest> findByAgentFixerIdOrderByDateCreatedDesc(Long agentFixerId);
    List<ClearanceRequest> findByAgentFixerIdAndRequestTypeOrderByDateCreatedDesc(Long agentFixerId, RequestType requestType);
    List<ClearanceRequest> findByRequestTypeOrderByDateCreatedDesc(RequestType requestType);
    Optional<ClearanceRequest> findByReferenceNo(String referenceNo);
    Optional<ClearanceRequest> findByVoucherCode(String voucherCode);
    Optional<ClearanceRequest> findByCertificateNo(String certificateNo);
}