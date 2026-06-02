package com.dci.clearance.repository;

import com.dci.clearance.entity.Purchase;
import com.dci.clearance.entity.Purchase.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByVoucherCode(String voucherCode);
    List<Purchase> findByPurchasedByIdOrderByPurchaseDateDesc(Long userId);
    List<Purchase> findByStatus(PurchaseStatus status);
}