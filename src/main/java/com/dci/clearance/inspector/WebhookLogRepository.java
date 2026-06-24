package com.dci.clearance.inspector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {
    List<WebhookLog> findTop50ByOrderByTimestampDesc();
}
