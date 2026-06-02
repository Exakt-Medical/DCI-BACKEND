package com.dci.clearance.service;

import com.dci.clearance.dto.VvsVehicleData;
import com.dci.clearance.dto.VehicleVerificationRequest;
import com.dci.clearance.dto.VehicleVerificationResponse;
import com.dci.clearance.entity.*;
import com.dci.clearance.entity.VerificationRequest.VerificationStatus;
import com.dci.clearance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRequestRepository verificationRepo;
    private final VerificationVvsLogRepository vvsLogRepo;
    private final VerificationVehicleDetailsRepository vehicleDetailsRepo;
    private final VerificationOwnerDetailsRepository ownerDetailsRepo;
    private final VvsApiClient vvsApiClient;
    private final DciCertificateService dciCertificateService;
    private final DciCertificateRepository dciCertificateRepo;
    private final VoucherService voucherService;

    @Transactional
    public VehicleVerificationResponse verify(VehicleVerificationRequest request, Long userId) {

        // CHECK FOR EXISTING CERTIFICATE FIRST
        if (request.getPlateNumber() != null && hasExistingCertificate(request.getPlateNumber())) {
            String reason = "THIS VEHICLE HAS ALREADY BEEN VERIFIED AND HAS AN EXISTING CERTIFICATE.";

            VerificationRequest record = buildPendingRecord(request, userId);
            record.setVerificationStatus(VerificationStatus.FAILED);
            record.setFailureReason(reason);
            verificationRepo.save(record);

            log.info("FAILED referenceNo={} — vehicle already has certificate", record.getReferenceNo());
            return VehicleVerificationResponse.failed(record.getReferenceNo(), reason);
        }

        VerificationRequest record = buildPendingRecord(request, userId);
        verificationRepo.save(record);

        VerificationVvsLog vvsLog = new VerificationVvsLog();
        vvsLog.setVerificationId(record.getId());

        try {
            String token = vvsApiClient.getToken();

            VvsVehicleData vehicleData = null;
            String usedEndpoint = null;

            String mvPlateRaw = tryGetByMvAndPlate(token, request, vvsLog);
            if (mvPlateRaw != null) {
                vehicleData = vvsApiClient.parseVehicleData(mvPlateRaw);
                usedEndpoint = "MV_PLATE";
            }

            if (vehicleData == null) {
                String engineChassisRaw = tryGetByEngineAndChassis(token, request, vvsLog);
                if (engineChassisRaw != null) {
                    vehicleData = vvsApiClient.parseVehicleData(engineChassisRaw);
                    usedEndpoint = "ENGINE_CHASSIS";
                }
            }

            vvsLog.setMatchedFieldNames(usedEndpoint);

            if (vehicleData != null) {
                vvsLog.setVvsRequestId(vehicleData.getRequestId());

                saveVehicleDetails(record.getId(), vehicleData);
                saveOwnerDetails(record.getId(), vehicleData);

                // Update verification request with vehicle details from VVS
                record.setMvFileNumber(vehicleData.getMvFileNo());
                record.setPlateNumber(vehicleData.getPlateNo());
                record.setChassisNumber(vehicleData.getChassisNo());
                record.setEngineNumber(vehicleData.getEngineNo());

                record.setVerificationStatus(VerificationStatus.VERIFIED);
                verificationRepo.save(record);
                vvsLogRepo.save(vvsLog);

                log.info("VERIFIED referenceNo={} via {}", record.getReferenceNo(), usedEndpoint);
                return VehicleVerificationResponse.verified(record.getReferenceNo(), record.getId(), vehicleData);

            } else {
                String reason = "No matching vehicle record found in VVS for the provided identifiers.";
                record.setVerificationStatus(VerificationStatus.FAILED);
                record.setFailureReason(reason);
                verificationRepo.save(record);
                vvsLogRepo.save(vvsLog);

                log.info("FAILED referenceNo={} — no VVS record returned", record.getReferenceNo());
                return VehicleVerificationResponse.failed(record.getReferenceNo(), reason);
            }

        } catch (VvsApiClient.VvsApiException e) {
            log.error("VVS API error referenceNo={}: {}", record.getReferenceNo(), e.getMessage());
            record.setVerificationStatus(VerificationStatus.ERROR);
            record.setFailureReason("VVS API error: " + e.getMessage());
            verificationRepo.save(record);
            vvsLogRepo.save(vvsLog);
            return VehicleVerificationResponse.error(record.getReferenceNo(), e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error referenceNo={}: {}", record.getReferenceNo(), e.getMessage());
            record.setVerificationStatus(VerificationStatus.ERROR);
            record.setFailureReason("Internal error: " + e.getMessage());
            verificationRepo.save(record);
            vvsLogRepo.save(vvsLog);
            return VehicleVerificationResponse.error(record.getReferenceNo(), "Internal processing error");
        }
    }

    @Transactional
    public VehicleVerificationResponse confirm(Long verificationId, VehicleVerificationRequest request, Long userId) {

        VerificationRequest record = verificationRepo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification record not found: " + verificationId));

        if (record.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Cannot confirm a record that is not VERIFIED. Current status: "
                    + record.getVerificationStatus());
        }

        VerificationVvsLog vvsLog = vvsLogRepo.findByVerificationId(verificationId)
                .orElse(new VerificationVvsLog());

        try {
            String token = vvsApiClient.getToken();

            String vvsRequestId = vvsLog.getVvsRequestId();
            if (vvsRequestId == null) {
                vvsRequestId = refetchRequestId(token, record, vvsLog);
            }

            LocalDate expiry = LocalDate.now().plusYears(1);
            String expiryStr = expiry.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

            String confirmResp = vvsApiClient.confirmRequest(token, vvsRequestId, record.getReferenceNo(), expiryStr);
            vvsLog.setVvsConfirmResponse(confirmResp);
            vvsLogRepo.save(vvsLog);

            if (confirmResp == null || confirmResp.contains("No matching record")) {
                throw new VvsApiClient.VvsApiException("VVS ConfirmRequest rejected: " + confirmResp);
            }

            VvsVehicleData vehicleData = resolveStoredVehicleData(vvsLog);

            String certNo = dciCertificateService.issue(
                    record, request.getPremiumType(), userId, expiry);

            String companyName = null;
            try {
                DciCertificate savedCert = dciCertificateRepo.findByCertificateNo(certNo).orElse(null);
                if (savedCert != null) companyName = savedCert.getCompanyName();
            } catch (Exception e) {
                log.warn("Could not fetch companyName for certNo={}", certNo);
            }

            String voucherCode = request.getVoucherCode();
            if (voucherCode != null && !voucherCode.isBlank()) {
                try {
                    voucherService.redeemVoucherByCode(voucherCode, certNo);
                    log.info("Voucher redeemed — voucherCode={} certNo={}", voucherCode, certNo);
                } catch (Exception e) {
                    log.error("Voucher redeem failed (cert still issued) — voucherCode={} error={}",
                            voucherCode, e.getMessage());
                }
            }

            record.setVerificationStatus(VerificationStatus.COMPLETED);
            verificationRepo.save(record);

            log.info("CONFIRMED referenceNo={} certNo={}", record.getReferenceNo(), certNo);
            return VehicleVerificationResponse.confirmed(record.getReferenceNo(), certNo, companyName);

        } catch (VvsApiClient.VvsApiException e) {
            log.error("ConfirmRequest failed referenceNo={}: {}", record.getReferenceNo(), e.getMessage());
            record.setFailureReason("Confirm error: " + e.getMessage());
            verificationRepo.save(record);
            return VehicleVerificationResponse.error(record.getReferenceNo(), e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected confirm error referenceNo={}: {}", record.getReferenceNo(), e.getMessage());
            record.setFailureReason("Internal confirm error: " + e.getMessage());
            verificationRepo.save(record);
            return VehicleVerificationResponse.error(record.getReferenceNo(), "Submission processing error");
        }
    }

    private boolean hasExistingCertificate(String plateNumber) {
        return verificationRepo.findByPlateNumberAndVerificationStatus(plateNumber, VerificationStatus.COMPLETED).isPresent();
    }

    private void saveVehicleDetails(Long verificationId, VvsVehicleData v) {
        VerificationVehicleDetails d = vehicleDetailsRepo.findByVerificationId(verificationId)
                .orElse(new VerificationVehicleDetails());
        d.setVerificationId(verificationId);
        d.setMake(v.getMake());
        d.setSeries(v.getSeries());
        d.setColor(v.getColor());
        d.setYearModel(v.getYearModel());
        d.setClassification(v.getClassification());
        d.setBodyType(v.getBodyType());
        d.setDenomination(v.getDenomination());
        d.setLastRegistrationDate(v.getLastRegistrationDate());
        vehicleDetailsRepo.save(d);
    }

    private void saveOwnerDetails(Long verificationId, VvsVehicleData v) {
        VerificationOwnerDetails d = ownerDetailsRepo.findByVerificationId(verificationId)
                .orElse(new VerificationOwnerDetails());
        d.setVerificationId(verificationId);
        d.setFirstName(v.getOwnerFirstName());
        d.setMiddleName(v.getOwnerMiddleName());
        d.setLastName(v.getOwnerLastName());
        d.setOrganization(v.getOwnerOrganization());
        d.setHouseBldgNo(v.getAddressHouseBldgNo());
        d.setStreetName(v.getAddressStreetName());
        d.setBarangay(v.getAddressBarangay());
        d.setMunicipality(v.getAddressMunicipality());
        d.setProvince(v.getAddressProvince());
        d.setRegion(v.getAddressRegion());
        d.setZipCode(v.getAddressZipCode());
        ownerDetailsRepo.save(d);
    }

    public VehicleVerificationResponse getByCertNo(String certNo) {
        DciCertificate cert = dciCertificateRepo.findByCertificateNo(certNo)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certNo));

        VerificationRequest record = verificationRepo.findById(cert.getVerificationId())
                .orElseThrow(() -> new IllegalArgumentException("Verification record not found for cert: " + certNo));

        VerificationVehicleDetails vehicleDetails = vehicleDetailsRepo
                .findByVerificationId(record.getId()).orElse(null);

        VerificationOwnerDetails ownerDetails = ownerDetailsRepo
                .findByVerificationId(record.getId()).orElse(null);

        return VehicleVerificationResponse.certificate(cert, record, vehicleDetails, ownerDetails);
    }

    private String tryGetByMvAndPlate(String token, VehicleVerificationRequest request, VerificationVvsLog vvsLog) {
        String raw = safeCall(() -> vvsApiClient.getByMvAndPlate(token, request.getMvFileNumber(), request.getPlateNumber()));
        vvsLog.setVvsMvPlateResponse(raw);
        return raw;
    }

    private String tryGetByEngineAndChassis(String token, VehicleVerificationRequest request, VerificationVvsLog vvsLog) {
        String raw = safeCall(() -> vvsApiClient.getByEngineAndChassis(token, request.getEngineNumber(), request.getChassisNumber()));
        vvsLog.setVvsEngineChassisResponse(raw);
        return raw;
    }

    private VvsVehicleData resolveStoredVehicleData(VerificationVvsLog vvsLog) {
        VvsVehicleData data = vvsApiClient.parseVehicleData(vvsLog.getVvsMvPlateResponse());
        if (data == null) data = vvsApiClient.parseVehicleData(vvsLog.getVvsEngineChassisResponse());
        return data;
    }

    private String refetchRequestId(String token, VerificationRequest record, VerificationVvsLog vvsLog) {
        String raw = safeCall(() -> vvsApiClient.getByMvAndPlate(token, record.getMvFileNumber(), record.getPlateNumber()));
        if (raw == null) raw = safeCall(() -> vvsApiClient.getByEngineAndChassis(token, record.getEngineNumber(), record.getChassisNumber()));
        VvsVehicleData data = vvsApiClient.parseVehicleData(raw);
        if (data != null) {
            vvsLog.setVvsRequestId(data.getRequestId());
            vvsLogRepo.save(vvsLog);
            return data.getRequestId();
        }
        return null;
    }

    private VerificationRequest buildPendingRecord(VehicleVerificationRequest req, Long userId) {
        VerificationRequest r = new VerificationRequest();
        r.setReferenceNo(generateReferenceNo());
        r.setMvFileNumber(req.getMvFileNumber());
        r.setPlateNumber(req.getPlateNumber());
        r.setChassisNumber(req.getChassisNumber());
        r.setEngineNumber(req.getEngineNumber());
        r.setRequestedBy(userId);
        r.setVerificationStatus(VerificationStatus.PENDING);
        return r;
    }

    private String generateReferenceNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("VVIP-%s-%06d", date, System.currentTimeMillis() % 1_000_000);
    }

    private String safeCall(java.util.concurrent.Callable<String> call) {
        try {
            return call.call();
        } catch (VvsApiClient.VvsApiException e) {
            String msg = e.getMessage();
            if (msg.contains("No matching record") || msg.contains("Invalid format"))
                log.debug("VVS returned no data (expected): {}", msg);
            else
                log.warn("VVS call failed (continuing): {}", msg);
            return null;
        } catch (Exception e) {
            log.warn("VVS call failed (continuing): {}", e.getMessage());
            return null;
        }
    }
}