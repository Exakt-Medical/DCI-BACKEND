package com.dci.clearance.repository;

import com.dci.clearance.entity.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    List<InsuranceProduct> findByIsActiveTrue();
}