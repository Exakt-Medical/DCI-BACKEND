package com.dci.clearance.repository;

import com.dci.clearance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByDateCreatedBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.dateCreated >= :start")
    long countToday(LocalDateTime start);
}