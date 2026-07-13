package com.dci.clearance.repository;

import com.dci.clearance.entity.CertificateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, Long> {
    List<CertificateRequest> findByUserIdOrderByDateUpdatedDesc(Long userId);
    Optional<CertificateRequest> findFirstByVoucherCodeOrderByIdDesc(String voucherCode);
    List<CertificateRequest> findByVoucherCodeIn(List<String> voucherCodes);

    @Query("SELECT cr FROM CertificateRequest cr " +
           "LEFT JOIN OrCrRequest orcr ON orcr.certificateRequest = cr " +
           "LEFT JOIN MvcMecRequest mvc ON mvc.certificateRequest = cr " +
           "WHERE cr.user.id = :userId " +
           "AND (:activeFilter = 'all' OR " +
           "     (:activeFilter = 'completed' AND cr.status = 'CERTIFICATE_ISSUED') OR " +
           "     (:activeFilter = 'voucher' AND (cr.status IS NULL OR cr.status NOT IN ('VOUCHER_ISSUED', 'HPG_VERIFIED', 'MVC_MEC_VALIDATED', 'CERTIFICATE_ISSUED'))) OR " +
           "     (:activeFilter = 'clearance' AND cr.status IN ('VOUCHER_ISSUED', 'HPG_VERIFIED', 'MVC_MEC_VALIDATED'))" +
           ") " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(CAST(cr.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(cr.voucherCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(cr.certificateNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(orcr.plateNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(mvc.plateNumber) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    Page<CertificateRequest> findPaginatedAndFiltered(
            @Param("userId") Long userId,
            @Param("activeFilter") String activeFilter,
            @Param("search") String search,
            Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(cr) FROM CertificateRequest cr WHERE cr.user.id = :userId AND cr.status = 'CERTIFICATE_ISSUED'")
    long countCompletedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(cr) FROM CertificateRequest cr WHERE cr.user.id = :userId AND (cr.status IS NULL OR cr.status NOT IN ('VOUCHER_ISSUED', 'HPG_VERIFIED', 'MVC_MEC_VALIDATED', 'CERTIFICATE_ISSUED'))")
    long countVoucherInProgressByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(cr) FROM CertificateRequest cr WHERE cr.user.id = :userId AND cr.status IN ('VOUCHER_ISSUED', 'HPG_VERIFIED', 'MVC_MEC_VALIDATED')")
    long countClearanceAwaitingByUserId(@Param("userId") Long userId);
}
