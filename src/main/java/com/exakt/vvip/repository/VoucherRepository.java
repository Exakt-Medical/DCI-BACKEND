package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface
VoucherRepository extends JpaRepository<Voucher, Long> {
    boolean existsByVoucherCode(String voucherCode);
    Optional<Voucher> findByVoucherCode(String voucherCode);
    long countByCurrentUserIdAndStatus(Long currentUserId, String status);
}
