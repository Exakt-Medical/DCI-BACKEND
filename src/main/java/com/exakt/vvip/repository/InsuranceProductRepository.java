package com.exakt.vvip.repository;

import com.exakt.vvip.entity.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    List<InsuranceProduct> findByIsActiveTrue();
}