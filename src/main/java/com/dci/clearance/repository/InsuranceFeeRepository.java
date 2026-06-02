package com.dci.clearance.repository;

import com.dci.clearance.entity.InsuranceFee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InsuranceFeeRepository extends JpaRepository<InsuranceFee, Long> {
    Optional<InsuranceFee> findByInsuranceCode(String insuranceCode);
}