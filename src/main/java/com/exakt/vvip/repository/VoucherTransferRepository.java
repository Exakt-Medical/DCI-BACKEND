package com.exakt.vvip.repository;

import com.exakt.vvip.entity.VoucherTransferEntity;
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

    List<VoucherTransferEntity> findByIdInAndCurrentUserIdAndStatus(
            List<Long> ids, Long currentUserId, String status);
}