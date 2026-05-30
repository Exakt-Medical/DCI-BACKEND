package com.exakt.vvip.repository;

import com.exakt.vvip.entity.VerificationRequest;
import com.exakt.vvip.entity.VerificationRequest.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {

    Optional<VerificationRequest> findByReferenceNo(String referenceNo);

    Optional<VerificationRequest> findByPlateNumberAndVerificationStatus(String plateNumber, VerificationStatus status);

    Page<VerificationRequest> findAllByOrderByDateCreatedDesc(Pageable pageable);

    Page<VerificationRequest> findByVerificationStatusOrderByDateCreatedDesc(VerificationStatus status, Pageable pageable);

    @Query("SELECT v FROM VerificationRequest v WHERE " +
            "LOWER(v.referenceNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY v.dateCreated DESC")
    Page<VerificationRequest> searchTransactions(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT v FROM VerificationRequest v WHERE " +
            "(:status IS NULL OR v.verificationStatus = :status) AND " +
            "(:searchTerm IS NULL OR LOWER(v.referenceNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY v.dateCreated DESC")
    Page<VerificationRequest> findWithFilters(@Param("status") VerificationStatus status,
                                              @Param("searchTerm") String searchTerm,
                                              Pageable pageable);

    long countByVerificationStatus(VerificationStatus status);

    // ADD THIS NEW METHOD FOR DATE-BASED AUTHENTICATION COUNTS
    @Query("SELECT COUNT(vr) FROM VerificationRequest vr WHERE vr.verificationStatus = 'COMPLETED' AND vr.dateCreated >= :since")
    long countAuthenticatedSince(@Param("since") LocalDateTime since);

    // UPDATED QUERY with vehicle details included
    @Query(value = "SELECT " +
            "vr.id, " +
            "vr.reference_no, " +
            "TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.middle_initial, ''), ' ', COALESCE(u.last_name, ''))) AS account, " +
            "CONCAT(COALESCE(c.company_name, b.branch_name), ' - ', b.branch_name) AS company, " +
            "vr.verification_status, " +
            "vr.plate_number, " +
            "vr.failure_reason, " +
            "cert.certificate_no, " +
            "vr.date_created, " +
            "vr.mv_file_number, " +
            "vr.chassis_number, " +
            "vr.engine_number " +
            "FROM verification_requests vr " +
            "LEFT JOIN users u ON u.id = vr.requested_by " +
            "LEFT JOIN branches b ON b.id = u.branch_id " +
            "LEFT JOIN companies c ON CAST(c.code AS CHAR) = CAST(b.company_id AS CHAR) " +
            "LEFT JOIN dci_certificates cert ON cert.verification_id = vr.id " +
            "WHERE (:status IS NULL " +
            "OR (:status = 'Authenticated' AND vr.verification_status = 'COMPLETED') " +
            "OR (:status = 'Verified' AND vr.verification_status = 'VERIFIED') " +
            "OR (:status = 'Failed' AND vr.verification_status IN ('FAILED','ERROR'))) " +
            "AND (:search IS NULL " +
            "OR LOWER(vr.reference_no) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(CONCAT(u.first_name,' ',u.last_name)) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(vr.plate_number) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY vr.date_created DESC",
            nativeQuery = true,
            countQuery = "SELECT COUNT(vr.id) FROM verification_requests vr " +
                    "LEFT JOIN users u ON u.id = vr.requested_by " +
                    "LEFT JOIN branches b ON b.id = u.branch_id " +
                    "LEFT JOIN companies c ON CAST(c.code AS CHAR) = CAST(b.company_id AS CHAR) " +
                    "WHERE (:status IS NULL " +
                    "OR (:status = 'Authenticated' AND vr.verification_status = 'COMPLETED') " +
                    "OR (:status = 'Verified' AND vr.verification_status = 'VERIFIED') " +
                    "OR (:status = 'Failed' AND vr.verification_status IN ('FAILED','ERROR'))) " +
                    "AND (:search IS NULL " +
                    "OR LOWER(vr.reference_no) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(CONCAT(u.first_name,' ',u.last_name)) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(vr.plate_number) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Object[]> findTransactionLogs(@Param("status") String status,
                                       @Param("search") String search,
                                       Pageable pageable);

    // Stats counts
    @Query("SELECT COUNT(vr) FROM VerificationRequest vr WHERE vr.verificationStatus = 'COMPLETED'")
    long countAuthenticated();

    @Query("SELECT COUNT(vr) FROM VerificationRequest vr WHERE vr.verificationStatus = 'VERIFIED'")
    long countVerified();

    @Query("SELECT COUNT(vr) FROM VerificationRequest vr WHERE vr.verificationStatus IN ('FAILED','ERROR')")
    long countFailed();
}