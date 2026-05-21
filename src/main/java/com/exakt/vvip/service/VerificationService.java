package com.exakt.vvip.service;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final int VERIFICATION_THRESHOLD = 2;

    private final VerificationRequestRepository     verificationRepo;
    private final VerificationVvsLogRepository vvsLogRepo;
    private final VerificationInsuranceRepository insuranceRepo;
    private final VvsApiClient                      vvsApiClient;
    private final DciCertificateService             dciCertificateService;

    @Transactional
    public VehicleVerificationResponse verify(VehicleVerificationRequest request, Long userId) {

        // 1. Save PENDING record
        VerificationRequest record = buildPendingRecord(request, userId);
        verificationRepo.save(record);

        // 2. Prepare VVS log (will be persisted at the end)
        VerificationVvsLog vvsLog = new VerificationVvsLog();
        vvsLog.setVerificationId(record.getId());

        try {
            // 3. Get token
            String token = vvsApiClient.getToken();
            vvsLog.setVvsToken(token);

            // 4. Call both VVS endpoints
            String mvPlateRaw       = safeCall(() -> vvsApiClient.getByMvAndPlate(
                    token, request.getMvFileNumber(), request.getPlateNumber()));
            String engineChassisRaw = safeCall(() -> vvsApiClient.getByEngineAndChassis(
                    token, request.getEngineNumber(), request.getChassisNumber()));

            vvsLog.setVvsMvPlateResponse(mvPlateRaw);
            vvsLog.setVvsEngineChassisResponse(engineChassisRaw);

            // 5. Parse
            VvsVehicleData mvPlateData       = vvsApiClient.parseVehicleData(mvPlateRaw);
            VvsVehicleData engineChassisData = vvsApiClient.parseVehicleData(engineChassisRaw);

            // 6. Evaluate 2-of-4
            List<String> matched = evaluate(request, mvPlateData, engineChassisData);
            vvsLog.setMatchedFields(matched.size());
            vvsLog.setMatchedFieldNames(String.join(", ", matched));

            if (matched.size() >= VERIFICATION_THRESHOLD) {

                record.setVerificationStatus(VerificationStatus.VERIFIED);
                verificationRepo.save(record);

                // 7. Confirm with VVS
                String vvsRequestId = extractRequestId(mvPlateData, engineChassisData);
                vvsLog.setVvsRequestId(vvsRequestId);

                LocalDate expiry = LocalDate.now().plusYears(1);
                String confirmResp = vvsApiClient.confirmRequest(
                        token, vvsRequestId, record.getReferenceNo(),
                        expiry.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                vvsLog.setVvsConfirmResponse(confirmResp);
                vvsLogRepo.save(vvsLog);

                // 8. Load insurance (premium type lives there now)
                VerificationInsurance insurance = insuranceRepo
                        .findByVerificationId(record.getId())
                        .orElse(null);

                // 9. Generate DCI certificate
                String certNo = dciCertificateService.issue(
                        record, mvPlateData, insurance, userId, expiry);

                log.info("VERIFIED referenceNo={} certNo={}", record.getReferenceNo(), certNo);
                return VehicleVerificationResponse.verified(
                        record.getReferenceNo(), matched.size(),
                        vvsLog.getMatchedFieldNames(), certNo);

            } else {
                String reason = "Only " + matched.size() + " of 4 identifiers matched. Minimum: "
                        + VERIFICATION_THRESHOLD;
                record.setVerificationStatus(VerificationStatus.FAILED);
                record.setFailureReason(reason);
                verificationRepo.save(record);
                vvsLogRepo.save(vvsLog);

                log.info("FAILED referenceNo={} matched={}", record.getReferenceNo(), matched.size());
                return VehicleVerificationResponse.failed(
                        record.getReferenceNo(), matched.size(),
                        vvsLog.getMatchedFieldNames(), reason);
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

    private List<String> evaluate(VehicleVerificationRequest req,
                                  VvsVehicleData mvPlate, VvsVehicleData engineChassis) {
        List<String> matched = new ArrayList<>();
        if (matches(req.getMvFileNumber(),   mvPlate      != null ? mvPlate.getMvFileNo()         : null)) matched.add("MV_FILE_NUMBER");
        if (matches(req.getPlateNumber(),    mvPlate      != null ? mvPlate.getPlateNo()           : null)) matched.add("PLATE_NUMBER");
        if (matches(req.getEngineNumber(),   engineChassis != null ? engineChassis.getEngineNo()   : null)) matched.add("ENGINE_NUMBER");
        if (matches(req.getChassisNumber(),  engineChassis != null ? engineChassis.getChassisNo()  : null)) matched.add("CHASSIS_NUMBER");
        return matched;
    }

    private boolean matches(String submitted, String fromVvs) {
        if (submitted == null || submitted.isBlank()) return false;
        if (fromVvs   == null || fromVvs.isBlank())   return false;
        return submitted.trim().equalsIgnoreCase(fromVvs.trim());
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

    private String extractRequestId(VvsVehicleData mvPlate, VvsVehicleData engineChassis) {
        if (mvPlate != null && mvPlate.getRequestId() != null) return mvPlate.getRequestId();
        if (engineChassis != null && engineChassis.getRequestId() != null) return engineChassis.getRequestId();
        return null;
    }

    private String safeCall(java.util.concurrent.Callable<String> call) {
        try {
            return call.call();
        } catch (Exception e) {
            log.warn("VVS partial call failed (continuing): {}", e.getMessage());
            return null;
        }
    }
}