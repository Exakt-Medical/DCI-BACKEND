package com.exakt.vvip.repository;

import com.exakt.vvip.entity.InsuranceFee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InsuranceFeeRepository extends JpaRepository<InsuranceFee, Long> {
    Optional<InsuranceFee> findByInsuranceCode(String insuranceCode);
}