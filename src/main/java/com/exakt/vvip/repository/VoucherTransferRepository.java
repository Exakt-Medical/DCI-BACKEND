package com.exakt.vvip.repository;

import com.exakt.vvip.entity.VoucherTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}