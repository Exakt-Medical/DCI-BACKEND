package com.dci.clearance.repository;

import com.dci.clearance.entity.AccessTrail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessTrailRepository extends JpaRepository<AccessTrail, Long> {
}
