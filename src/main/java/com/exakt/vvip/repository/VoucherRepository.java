package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    boolean existsByVoucherCode(String voucherCode);
}
