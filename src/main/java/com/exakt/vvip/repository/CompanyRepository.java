package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Company;
import com.exakt.vvip.entity.Company.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByStatus(CompanyStatus status);
    long countByStatus(CompanyStatus status);
}