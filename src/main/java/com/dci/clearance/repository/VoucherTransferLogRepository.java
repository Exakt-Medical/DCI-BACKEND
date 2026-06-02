package com.dci.clearance.repository;

import com.dci.clearance.entity.VoucherTransferLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherTransferLogRepository extends JpaRepository<VoucherTransferLog, Long> {

    // All logs where this user was the sender — for manager history view
    List<VoucherTransferLog> findByFromUserIdOrderByTransferredAtDesc(Long fromUserId);

    // All logs for a specific transfer batch (same reference number)
    List<VoucherTransferLog> findByReferenceNumber(String referenceNumber);

    // Count how many vouchers a manager has transferred to a specific agent
    long countByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    // Distinct agents a manager has ever transferred to
    @Query("SELECT DISTINCT l.toUserId FROM VoucherTransferLog l WHERE l.fromUserId = :fromUserId")
    List<Long> findDistinctToUserIdsByFromUserId(@Param("fromUserId") Long fromUserId);
}