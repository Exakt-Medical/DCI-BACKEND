package com.exakt.vvip.repository;

import com.exakt.vvip.entity.TransactionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    Page<TransactionLog> findByStatus(String status, Pageable pageable);

    @Query("SELECT t FROM TransactionLog t WHERE " +
            "LOWER(t.account) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.company) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.refNo) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TransactionLog> searchAllFields(@Param("search") String search, Pageable pageable);
}