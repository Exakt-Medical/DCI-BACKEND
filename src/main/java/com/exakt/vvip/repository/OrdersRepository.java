package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Optional<Orders> findByMerchantReferenceId(String merchantReferenceId);

    Optional<Orders> findByTlpeTransactionId(String tlpeTransactionId);

    List<Orders> findByStatus(String status);

    List<Orders> findByUserId(Long userId);
}

