package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    java.util.Optional<Company> findByCompanyId(String companyId);
}
