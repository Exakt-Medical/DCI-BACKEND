package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying
    @Query("""
        UPDATE Order o SET 
            o.billerooConfirmed = true,
            o.billerooConfirmedAt = CURRENT_TIMESTAMP,
            o.status = 'BILLEROO_CONFIRMED',
            o.updatedAt = CURRENT_TIMESTAMP
        WHERE o.id = :orderId
    """)
    void updateBillerooConfirmed(@Param("orderId") Long orderId);

    @Modifying
    @Query("UPDATE Order o SET o.status = 'COMPLETED', o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :orderId")
    void markCompleted(@Param("orderId") Long orderId);

    @Modifying
    @Query("UPDATE Order o SET o.status = 'FAILED', o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :orderId")
    void markFailed(@Param("orderId") Long orderId);
}
