package com.exakt.vvip.service;

import com.exakt.vvip.dto.VvsVehicleData;
import com.exakt.vvip.entity.DciCertificate;
import com.exakt.vvip.entity.VerificationInsurance;
import com.exakt.vvip.entity.VerificationRequest;
import com.exakt.vvip.repository.DciCertificateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class DciCertificateService {

    private final DciCertificateRepository certRepo;

    public DciCertificateService(DciCertificateRepository certRepo) {
        this.certRepo = certRepo;
    }

    public String issue(VerificationRequest record, VvsVehicleData vehicleData,
                        VerificationInsurance insurance,
                        Long issuedBy, LocalDate expiryDate) {
        String certNo = generateCertNo();

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
        cert.setPdfFilePath(null);
        cert.setIssuedBy(issuedBy);
        certRepo.save(cert);

        log.info("DCI certificate record saved: {}", certNo);
        return certNo;
    }

    private String generateCertNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("DCI-%s-%06d", date, System.currentTimeMillis() % 1_000_000);
    }
}