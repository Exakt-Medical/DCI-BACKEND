package com.exakt.vvip.repository;

import com.exakt.vvip.entity.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
}
