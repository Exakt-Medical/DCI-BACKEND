package com.dci.clearance.repository;

import com.dci.clearance.entity.VoucherTransferEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherTransferRepository extends JpaRepository<VoucherTransferEntity, Long> {
    List<VoucherTransferEntity> findByCurrentUserId(Long currentUserId);
    List<VoucherTransferEntity> findByOriginalUserId(Long originalUserId);
    List<VoucherTransferEntity> findByStatus(String status);
    List<VoucherTransferEntity> findByCurrentUserIdAndStatus(Long currentUserId, String status);
    long countByCurrentUserIdAndStatus(Long currentUserId, String status);
    long countByCurrentUserId(Long currentUserId);

    @Query("SELECT v FROM VoucherTransferEntity v WHERE v.currentUserId = :userId AND v.status = 'AVAILABLE' " +
            "AND (:search = '' OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<VoucherTransferEntity> findAvailableByUserIdPaginated(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT v FROM VoucherTransferEntity v WHERE v.currentUserId = :userId " +
           "AND (:activeFilter = 'ALL' OR " +
           "     (:activeFilter = 'AVAILABLE' AND v.status = 'AVAILABLE' AND NOT EXISTS (SELECT 1 FROM CertificateRequest cr WHERE cr.voucherCode = v.voucherCode)) OR " +
           "     (:activeFilter = 'ASSIGNED'  AND v.status = 'AVAILABLE' AND EXISTS (SELECT 1 FROM CertificateRequest cr WHERE cr.voucherCode = v.voucherCode)) OR " +
           "     (:activeFilter = 'USED'      AND v.status = 'REDEEMED') OR " +
           "     (:activeFilter = 'EXPIRED'   AND v.status = 'EXPIRED')" +
           ") " +
           "AND (:search IS NULL OR :search = '' OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<VoucherTransferEntity> findPaginatedAndFiltered(
            @Param("userId") Long userId,
            @Param("activeFilter") String activeFilter,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(v) FROM VoucherTransferEntity v WHERE v.currentUserId = :userId AND v.status = 'AVAILABLE' AND NOT EXISTS (SELECT 1 FROM CertificateRequest cr WHERE cr.voucherCode = v.voucherCode)")
    long countStrictlyAvailableByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(v) FROM VoucherTransferEntity v WHERE v.currentUserId = :userId AND v.status = 'AVAILABLE' AND EXISTS (SELECT 1 FROM CertificateRequest cr WHERE cr.voucherCode = v.voucherCode)")
    long countAssignedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(v) FROM VoucherTransferEntity v WHERE v.currentUserId = :userId AND v.status = 'REDEEMED'")
    long countUsedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(v) FROM VoucherTransferEntity v WHERE v.currentUserId = :userId AND v.status = 'EXPIRED'")
    long countExpiredByUserId(@Param("userId") Long userId);

    List<VoucherTransferEntity> findByIdInAndCurrentUserIdAndStatus(
            List<Long> ids, Long currentUserId, String status);

    // ✅ Batch count — one query for all agents instead of N individual queries
    // Returns [currentUserId, availableCount] pairs for every userId in the list
    @Query("SELECT v.currentUserId, COUNT(v) FROM VoucherTransferEntity v " +
            "WHERE v.currentUserId IN :userIds AND v.status = 'AVAILABLE' " +
            "GROUP BY v.currentUserId")
    List<Object[]> countAvailableByUserIds(@Param("userIds") List<Long> userIds);
}