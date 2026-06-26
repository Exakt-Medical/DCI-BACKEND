package com.dci.clearance.repository;

import com.dci.clearance.entity.CertificateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, Long> {
    List<CertificateRequest> findByUserIdOrderByDateUpdatedDesc(Long userId);
    Optional<CertificateRequest> findFirstByVoucherCodeOrderByIdDesc(String voucherCode);
    List<CertificateRequest> findByVoucherCodeIn(List<String> voucherCodes);
}
