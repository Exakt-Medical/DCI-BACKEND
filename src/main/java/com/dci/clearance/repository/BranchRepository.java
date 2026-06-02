package com.dci.clearance.repository;

import com.dci.clearance.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByCompanyId(Long companyId);
    java.util.Optional<Branch> findByBranchId(String branchId);
}








