package com.dci.clearance.repository;

import com.dci.clearance.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    java.util.Optional<Company> findByCode(String code);

    boolean existsByCode(String code);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Company c SET c.availableVouchers = c.availableVouchers + :count WHERE c.id = :companyId")
    void incrementAvailableVouchers(@org.springframework.data.repository.query.Param("companyId") Long companyId, @org.springframework.data.repository.query.Param("count") int count);

    @org.springframework.data.jpa.repository.Query("SELECT c.availableVouchers FROM Company c WHERE c.id = :companyId")
    Integer getAvailableVouchers(@org.springframework.data.repository.query.Param("companyId") Long companyId);
}
