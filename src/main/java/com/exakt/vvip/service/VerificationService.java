package com.exakt.vvip.service;

import com.exakt.vvip.dto.VvsLookupResponse;
import com.exakt.vvip.dto.VvsVehicleData;
import com.exakt.vvip.dto.VehicleVerificationRequest;
import com.exakt.vvip.dto.VehicleVerificationResponse;
import com.exakt.vvip.entity.VerificationInsurance;
import com.exakt.vvip.entity.VerificationRequest;
import com.exakt.vvip.entity.VerificationRequest.VerificationStatus;
import com.exakt.vvip.entity.VerificationVvsLog;
import com.exakt.vvip.repository.VerificationInsuranceRepository;
import com.exakt.vvip.repository.VerificationRequestRepository;
import com.exakt.vvip.repository.VerificationVvsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRequestRepository   verificationRepo;
    private final VerificationVvsLogRepository    vvsLogRepo;
    private final VerificationInsuranceRepository insuranceRepo;
    private final VvsApiClient                    vvsApiClient;
    private final DciCertificateService           dciCertificateService;

    // -------------------------------------------------------------------------
    // STEP 2-3 — GetDetails: choose ONE endpoint, verify on API success
    // -------------------------------------------------------------------------

    @Transactional
    public VehicleVerificationResponse verify(VehicleVerificationRequest request, Long userId) {

        VerificationRequest record = buildPendingRecord(request, userId);
        verificationRepo.save(record);

        VerificationVvsLog vvsLog = new VerificationVvsLog();
        vvsLog.setVerificationId(record.getId());

        try {
            String token = vvsApiClient.getToken();
            vvsLog.setVvsToken(token);

            // Try MV+Plate first
            VvsVehicleData vehicleData  = null;
            String         usedEndpoint = null;

            String mvPlateRaw = tryGetByMvAndPlate(token, request, vvsLog);
            if (mvPlateRaw != null) {
                vehicleData  = vvsApiClient.parseVehicleData(mvPlateRaw);
                usedEndpoint = "MV_PLATE";
            }

            // Fall back to Engine+Chassis
            if (vehicleData == null) {
                String engineChassisRaw = tryGetByEngineAndChassis(token, request, vvsLog);
                if (engineChassisRaw != null) {
                    vehicleData  = vvsApiClient.parseVehicleData(engineChassisRaw);
                    usedEndpoint = "ENGINE_CHASSIS";
                }
            }

            vvsLog.setMatchedFieldNames(usedEndpoint);

            if (vehicleData != null) {
                // Store the VVS request ID now so confirm() can use it later
                vvsLog.setVvsRequestId(vehicleData.getRequestId());

                record.setVerificationStatus(VerificationStatus.VERIFIED);
                verificationRepo.save(record);
                vvsLogRepo.save(vvsLog);

                log.info("VERIFIED referenceNo={} via {}", record.getReferenceNo(), usedEndpoint);
                return VehicleVerificationResponse.verified(
                        record.getReferenceNo(),
                        record.getId(),
                        vehicleData);

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

    // -------------------------------------------------------------------------
    // STEP 5-6 — Final Review submit: ConfirmRequest → generate certificate
    // -------------------------------------------------------------------------

    @Transactional
    public VehicleVerificationResponse confirm(Long verificationId, VehicleVerificationRequest request, Long userId) {

        VerificationRequest record = verificationRepo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Verification record not found: " + verificationId));

        if (record.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Cannot confirm a record that is not VERIFIED. Current status: "
                            + record.getVerificationStatus());
        }

        // Save insurance data from the final review form
        saveInsurance(verificationId, request);

        VerificationVvsLog vvsLog = vvsLogRepo.findByVerificationId(verificationId)
                .orElse(new VerificationVvsLog());

        try {
            String token = vvsApiClient.getToken();

            String vvsRequestId = vvsLog.getVvsRequestId();
            if (vvsRequestId == null) {
                vvsRequestId = refetchRequestId(token, record, vvsLog);
            }

            LocalDate expiry    = LocalDate.now().plusYears(1);
            String    expiryStr = expiry.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

            String confirmResp = vvsApiClient.confirmRequest(
                    token, vvsRequestId, record.getReferenceNo(), expiryStr);
            vvsLog.setVvsConfirmResponse(confirmResp);
            vvsLogRepo.save(vvsLog);

            if (confirmResp == null || confirmResp.contains("No matching record")) {
                throw new VvsApiClient.VvsApiException("VVS ConfirmRequest rejected: " + confirmResp);
            }

            VvsVehicleData vehicleData = resolveStoredVehicleData(vvsLog);

            VerificationInsurance insurance = insuranceRepo
                    .findByVerificationId(record.getId())
                    .orElse(null);

            String certNo = dciCertificateService.issue(
                    record, vehicleData, insurance, userId, expiry);

            record.setVerificationStatus(VerificationStatus.COMPLETED);
            verificationRepo.save(record);

            log.info("CONFIRMED referenceNo={} certNo={}", record.getReferenceNo(), certNo);
            return VehicleVerificationResponse.confirmed(record.getReferenceNo(), certNo);

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

//    public VvsLookupResponse lookup(VehicleVerificationRequest request) {
//        try {
//            String token = vvsApiClient.getToken();
//
//            String mvPlateRaw = safeCall(() -> vvsApiClient.getByMvAndPlate(
//                    token, request.getMvFileNumber(), request.getPlateNumber()));
//
//            VvsVehicleData data = vvsApiClient.parseVehicleData(mvPlateRaw);
//
//            if (data == null) {
//                String engineChassisRaw = safeCall(() -> vvsApiClient.getByEngineAndChassis(
//                        token, request.getEngineNumber(), request.getChassisNumber()));
//                data = vvsApiClient.parseVehicleData(engineChassisRaw);
//            }
//
//            if (data == null) return VvsLookupResponse.builder().found(false).build();
//
//            return VvsLookupResponse.builder()
//                    .found(true)
//                    .mvFileNumber(data.getMvFileNo())
//                    .plateNumber(data.getPlateNo())
//                    .engineNumber(data.getEngineNo())
//                    .chassisNumber(data.getChassisNo())
//                    .make(data.getMake())
//                    .series(data.getSeries())
//                    .color(data.getColor())
//                    .yearModel(data.getYearModel())
//                    .bodyType(data.getBodyType())
//                    .ownerFullName(data.getFullOwnerName())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Lookup failed: {}", e.getMessage());
//            return VvsLookupResponse.builder().found(false).build();
//        }
//    }

    private void saveInsurance(Long verificationId, VehicleVerificationRequest request) {
        VerificationInsurance ins = insuranceRepo.findByVerificationId(verificationId)
                .orElse(new VerificationInsurance());

        ins.setVerificationId(verificationId);
        ins.setInsuranceCode(request.getInsuranceCode());
        ins.setPolicyNumber(request.getPolicyNumber());
        ins.setPremiumType(request.getPremiumType());
        ins.setVoucherCode(request.getVoucherCode());

        ins.setPrescribedPremiumFee(parseBigDecimal(request.getPrescribedPremiumFee()));
        ins.setDst(parseBigDecimal(request.getDst()));
        ins.setVat(parseBigDecimal(request.getVat()));
        ins.setLgt(parseBigDecimal(request.getLgt()));
        ins.setValidationFee(parseBigDecimal(request.getValidationFee()));
        ins.setTotalAmount(parseBigDecimal(request.getTotalAmount()));

        insuranceRepo.save(ins);
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return (value != null && !value.isBlank()) ? new BigDecimal(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String tryGetByMvAndPlate(String token,
                                      VehicleVerificationRequest request,
                                      VerificationVvsLog vvsLog) {
        String raw = safeCall(() -> vvsApiClient.getByMvAndPlate(
                token, request.getMvFileNumber(), request.getPlateNumber()));
        vvsLog.setVvsMvPlateResponse(raw);
        return raw;
    }

    private String tryGetByEngineAndChassis(String token,
                                            VehicleVerificationRequest request,
                                            VerificationVvsLog vvsLog) {
        String raw = safeCall(() -> vvsApiClient.getByEngineAndChassis(
                token, request.getEngineNumber(), request.getChassisNumber()));
        vvsLog.setVvsEngineChassisResponse(raw);
        return raw;
    }

    private VvsVehicleData resolveStoredVehicleData(VerificationVvsLog vvsLog) {
        VvsVehicleData data = vvsApiClient.parseVehicleData(vvsLog.getVvsMvPlateResponse());
        if (data == null) {
            data = vvsApiClient.parseVehicleData(vvsLog.getVvsEngineChassisResponse());
        }
        return data;
    }

    private String refetchRequestId(String token,
                                    VerificationRequest record,
                                    VerificationVvsLog vvsLog) {
        String raw = safeCall(() -> vvsApiClient.getByMvAndPlate(
                token, record.getMvFileNumber(), record.getPlateNumber()));
        if (raw == null) {
            raw = safeCall(() -> vvsApiClient.getByEngineAndChassis(
                    token, record.getEngineNumber(), record.getChassisNumber()));
        }
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
            if (msg.contains("No matching record") || msg.contains("Invalid format")) {
                log.debug("VVS returned no data (expected): {}", msg);
            } else {
                log.warn("VVS call failed (continuing): {}", msg);
            }
            return null;
        } catch (Exception e) {
            log.warn("VVS call failed (continuing): {}", e.getMessage());
            return null;
        }
    }
}