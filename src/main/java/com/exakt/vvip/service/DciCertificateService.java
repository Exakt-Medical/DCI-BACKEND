package com.exakt.vvip.service;

import com.exakt.vvip.dto.VvsVehicleData;
import com.exakt.vvip.entity.DciCertificate;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.entity.VerificationInsurance;
import com.exakt.vvip.entity.VerificationRequest;
import com.exakt.vvip.repository.DciCertificateRepository;
import com.exakt.vvip.repository.UserRepository;
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

    public String issue(VerificationRequest record, VvsVehicleData vehicleData,
                        String premiumType, Long issuedBy, LocalDate expiryDate) {

        String certNo      = generateCertNo();
        String issuerName  = null;
        String companyName = null;

        if (issuedBy != null) {
            User user = userRepo.findById(issuedBy).orElse(null);
            if (user != null) {
                issuerName = Stream.of(user.getFirstName(), user.getMiddleInitial(), user.getLastName())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(" "));
                if (user.getBranch() != null && user.getBranch().getCompany() != null) {
                    companyName = user.getBranch().getCompany().getCompanyName();
                }
            }
        }

        DciCertificate cert = new DciCertificate();
        cert.setCertificateNo(certNo);
        cert.setVerificationId(record.getId());
        cert.setMvFileNumber(record.getMvFileNumber());
        cert.setPlateNumber(record.getPlateNumber());
        cert.setChassisNumber(record.getChassisNumber());
        cert.setEngineNumber(record.getEngineNumber());
        cert.setOwnerName(vehicleData != null ? vehicleData.getFullOwnerName() : null);
        cert.setIssuedDate(LocalDate.now());
        cert.setExpiryDate(expiryDate);
        cert.setIssuedBy(issuedBy);
        cert.setIssuerName(issuerName);
        cert.setCompanyName(companyName);
        cert.setPremiumType(premiumType);
        cert.setPdfFilePath(null);
        certRepo.save(cert);

        log.info("DCI certificate record saved: {}", certNo);
        return certNo;
    }

    private String generateCertNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("DCI-%s-%06d", date, System.currentTimeMillis() % 1_000_000);
    }
}