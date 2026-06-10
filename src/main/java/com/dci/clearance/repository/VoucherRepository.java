package com.dci.clearance.repository;

import com.dci.clearance.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface
VoucherRepository extends JpaRepository<Voucher, Long> {
    boolean existsByVoucherCode(String voucherCode);
    Optional<Voucher> findByVoucherCode(String voucherCode);
    Optional<Voucher> findByVoucherReference(String voucherReference);
    long countByCurrentUserIdAndStatus(Long currentUserId, String status);
}
