package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, Long> {
    Optional<Payments> findByTransactionId(Long transactionId);
    List<Payments> findByStatus(String status);
}
