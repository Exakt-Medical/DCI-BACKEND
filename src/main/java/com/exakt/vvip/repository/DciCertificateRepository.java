package com.exakt.vvip.repository;

import com.exakt.vvip.entity.DciCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DciCertificateRepository extends JpaRepository<DciCertificate, Long> {

    Optional<DciCertificate> findByCertificateNo(String certificateNo);

    Optional<DciCertificate> findByVerificationId(Long verificationId);
}