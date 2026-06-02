package com.dci.clearance.service;

import com.dci.clearance.entity.DciCertificate;
import com.dci.clearance.entity.User;
import com.dci.clearance.entity.VerificationRequest;
import com.dci.clearance.repository.DciCertificateRepository;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DciCertificateService {

    private final DciCertificateRepository certRepo;
    private final UserRepository userRepo;

    public String issue(VerificationRequest record,
                        String premiumType, Long issuedBy, LocalDate expiryDate) {

        String certNo = generateCertNo();
        String issuerName = null;
        String companyName = null;

        if (issuedBy != null) {
            User user = userRepo.findById(issuedBy).orElse(null);
            if (user != null) {
                issuerName = Stream.of(user.getFirstName(), user.getLastName())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(" "));
            }
        }

        DciCertificate cert = new DciCertificate();
        cert.setCertificateNo(certNo);
        cert.setVerificationId(record.getId());
        cert.setIssuedDate(LocalDate.now());
        cert.setExpiryDate(expiryDate);
        cert.setIssuedBy(issuedBy);
        cert.setIssuerName(issuerName);
        cert.setCompanyName(companyName);
        cert.setPremiumType(premiumType);
        certRepo.save(cert);

        log.info("DCI certificate record saved: {}", certNo);
        return certNo;
    }

    private String generateCertNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("DCI-%s-%06d", date, System.currentTimeMillis() % 1_000_000);
    }
}