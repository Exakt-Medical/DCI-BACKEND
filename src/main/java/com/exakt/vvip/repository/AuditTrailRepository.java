package com.exakt.vvip.repository;

import com.exakt.vvip.entity.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {

    @Query("SELECT DISTINCT a.actionMade FROM AuditTrail a ORDER BY a.actionMade")
    List<String> findDistinctActionMades();

    @Query("SELECT DISTINCT a.userstamp FROM AuditTrail a WHERE a.userstamp IS NOT NULL ORDER BY a.userstamp")
    List<String> findDistinctUserstamps();
}