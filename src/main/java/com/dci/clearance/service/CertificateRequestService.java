package com.dci.clearance.service;

import com.dci.clearance.entity.User;
import com.dci.clearance.entity.CertificateRequest;
import com.dci.clearance.entity.Voucher;
import com.dci.clearance.entity.VerificationVehicleDetails;
import com.dci.clearance.entity.VerificationOwnerDetails;
import com.dci.clearance.entity.VerificationRequest;
import com.dci.clearance.entity.OrCrRequest;
import com.dci.clearance.entity.MvcMecRequest;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.repository.CertificateRequestRepository;
import com.dci.clearance.repository.VoucherRepository;
import com.dci.clearance.repository.VerificationVehicleDetailsRepository;
import com.dci.clearance.repository.VerificationOwnerDetailsRepository;
import com.dci.clearance.repository.VerificationRequestRepository;
import com.dci.clearance.repository.OrCrRequestRepository;
import com.dci.clearance.repository.MvcMecRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CertificateRequestService {

    private final CertificateRequestRepository repository;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final VerificationVehicleDetailsRepository vehicleDetailsRepo;
    private final VerificationOwnerDetailsRepository ownerDetailsRepo;
    private final VerificationRequestRepository verificationRequestRepo;
    private final VoucherService voucherService;
    private final OrCrRequestRepository orCrRequestRepository;
    private final MvcMecRequestRepository mvcMecRequestRepository;

    public List<CertificateRequest> getMyRequests(Long userId) {
        return repository.findByUserIdOrderByDateUpdatedDesc(userId);
    }

    @Transactional
    public CertificateRequest upsertRequest(Long userId, Map<String, Object> payload) {
        Object idObj = payload.get("id");
        Long id = null;
        if (idObj != null) {
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String && !((String) idObj).isEmpty()) {
                try {
                    id = Long.parseLong((String) idObj);
                } catch (NumberFormatException ignored) {}
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CertificateRequest record = null;
        if (id != null) {
            record = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Record not found"));
        }

        if (record != null) {
            if (!record.getUser().getId().equals(userId)) {
                throw new RuntimeException("Unauthorized to modify this request");
            }
        } else {
            record = new CertificateRequest();
            record.setUser(user);
        }

        Object stepObj = payload.get("currentStep");
        if (stepObj != null) {
            if (stepObj instanceof Number) {
                record.setCurrentStep(((Number) stepObj).intValue());
            } else if (stepObj instanceof String && !((String) stepObj).isEmpty()) {
                try {
                    record.setCurrentStep(Integer.parseInt((String) stepObj));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (payload.get("status") != null) {
            record.setStatus((String) payload.get("status"));
        }
        if (payload.get("certificateNo") != null) {
            record.setCertificateNo((String) payload.get("certificateNo"));
        }

        if (payload.get("voucherCode") != null) {
            record.setVoucherCode((String) payload.get("voucherCode"));
        }

        if (payload.get("voucherId") != null) {
            Long voucherId = null;
            Object vId = payload.get("voucherId");
            if (vId instanceof Number) {
                voucherId = ((Number) vId).longValue();
            } else if (vId instanceof String && !((String) vId).isEmpty()) {
                voucherId = Long.parseLong((String) vId);
            }
            if (voucherId != null) {
                java.util.Optional<Voucher> vOpt = voucherRepository.findById(voucherId);
                if (vOpt.isPresent()) {
                    Voucher v = vOpt.get();
                    record.setVoucher(v);
                }
            }
        } else if (record.getVoucher() == null && record.getVoucherCode() != null && !record.getVoucherCode().isBlank()) {
            java.util.Optional<Voucher> vOpt = voucherRepository.findByVoucherCode(record.getVoucherCode());
            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();
                record.setVoucher(v);
            }
        }

        Long verificationId = null;
        Object newVIdObj = payload.get("verificationId");
        if (newVIdObj != null) {
            if (newVIdObj instanceof Number) {
                verificationId = ((Number) newVIdObj).longValue();
            } else if (newVIdObj instanceof String && !((String) newVIdObj).isEmpty()) {
                try {
                    verificationId = Long.parseLong((String) newVIdObj);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (verificationId != null) {
            record.setVerificationId(verificationId);
        }

        CertificateRequest savedRecord = repository.save(record);

        if ("CERTIFICATE_ISSUED".equals(payload.get("status"))) {
            if (savedRecord.getCertificateNo() == null || savedRecord.getCertificateNo().isBlank() || savedRecord.getCertificateNo().startsWith("DCI-CERT-17") || savedRecord.getCertificateNo().startsWith("DCI-CERT-18")) {
                long randomNum = java.util.concurrent.ThreadLocalRandom.current().nextLong(10000000L, 100000000L);
                String certNo = String.format("DCI-CERT-%d%04d", randomNum, savedRecord.getId());
                savedRecord.setCertificateNo(certNo);
                savedRecord = repository.save(savedRecord);
            }
        }
        // 1. Persist OR/CR details if present
        if (payload.containsKey("orCr") || payload.containsKey("crCr") || payload.containsKey("vehicleOption")) {
            OrCrRequest orCrReq = orCrRequestRepository.findByCertificateRequestId(savedRecord.getId())
                    .orElse(new OrCrRequest());
            orCrReq.setCertificateRequest(savedRecord);

            if (payload.get("vehicleOption") != null) orCrReq.setVehicleOption((String) payload.get("vehicleOption"));


            if (payload.containsKey("orCr")) {
                Map<?, ?> orMap = (Map<?, ?>) payload.get("orCr");
                if (orMap.get("plateNumber") != null && !((String)orMap.get("plateNumber")).isEmpty()) {
                    orCrReq.setPlateNumber((String) orMap.get("plateNumber"));
                }
                if (orMap.get("mvFileNumber") != null && !((String)orMap.get("mvFileNumber")).isEmpty()) {
                    orCrReq.setMvFileNumber((String) orMap.get("mvFileNumber"));
                }
            }
            if (payload.containsKey("crCr")) {
                Map<?, ?> crMap = (Map<?, ?>) payload.get("crCr");
                if (crMap.get("plateNumber") != null && !((String)crMap.get("plateNumber")).isEmpty()) {
                    orCrReq.setPlateNumber((String) crMap.get("plateNumber"));
                }
                if (crMap.get("mvFileNumber") != null && !((String)crMap.get("mvFileNumber")).isEmpty()) {
                    orCrReq.setMvFileNumber((String) crMap.get("mvFileNumber"));
                }
                if (crMap.get("engineNumber") != null) {
                    orCrReq.setEngineNumber((String) crMap.get("engineNumber"));
                }
                if (crMap.get("chassisNumber") != null) {
                    orCrReq.setChassisNumber((String) crMap.get("chassisNumber"));
                }
            }
            orCrRequestRepository.save(orCrReq);

            // Step 2 VVS Validation Check!
            if ("DOCUMENTS_VERIFIED".equals(payload.get("status"))) {
                String plate = orCrReq.getPlateNumber() != null ? orCrReq.getPlateNumber().trim().toUpperCase() : "";
                String engine = orCrReq.getEngineNumber() != null ? orCrReq.getEngineNumber().trim().toUpperCase() : "";
                String chassis = orCrReq.getChassisNumber() != null ? orCrReq.getChassisNumber().trim().toUpperCase() : "";
                String mvFile = orCrReq.getMvFileNumber() != null ? orCrReq.getMvFileNumber().trim().toUpperCase() : "";

                Optional<VerificationRequest> vvsOpt = Optional.empty();
                if (!plate.isEmpty()) {
                    vvsOpt = verificationRequestRepo.findFirstByPlateNumberAndVerificationStatusOrderByIdDesc(
                            plate, VerificationRequest.VerificationStatus.VERIFIED);
                }
                if (vvsOpt.isEmpty() && !engine.isEmpty() && !chassis.isEmpty()) {
                    vvsOpt = verificationRequestRepo.findFirstByEngineNumberAndChassisNumberAndVerificationStatusOrderByIdDesc(
                            engine, chassis, VerificationRequest.VerificationStatus.VERIFIED);
                }

                if (vvsOpt.isEmpty()) {
                    savedRecord.setStatus("Unverified");
                    repository.save(savedRecord);
                    throw new RuntimeException("DCI validation failed: No matching verified vehicle record found in VVS system.");
                }

                VerificationRequest vr = vvsOpt.get();
                VerificationVehicleDetails vd = vehicleDetailsRepo.findByVerificationId(vr.getId()).orElse(null);
                VerificationOwnerDetails od = ownerDetailsRepo.findByVerificationId(vr.getId()).orElse(null);

                boolean match = true;
                String mismatchReason = "";

                if (vd != null) {
                    String vvsEngine = vr.getEngineNumber() != null ? vr.getEngineNumber() : "";
                    String vvsChassis = vr.getChassisNumber() != null ? vr.getChassisNumber() : "";
                    String vvsPlate = vr.getPlateNumber() != null ? vr.getPlateNumber() : "";
                    String vvsMvFile = vr.getMvFileNumber() != null ? vr.getMvFileNumber() : "";
                    java.util.List<String> mismatches = new java.util.ArrayList<>();
                    if (!engine.equalsIgnoreCase(vvsEngine)) { mismatches.add("Engine Number mismatch"); }
                    if (!chassis.equalsIgnoreCase(vvsChassis)) { mismatches.add("Chassis Number mismatch"); }
                    if (!plate.isEmpty() && !plate.equalsIgnoreCase(vvsPlate)) { mismatches.add("Plate Number mismatch"); }
                    if (!mvFile.isEmpty() && !mvFile.equalsIgnoreCase(vvsMvFile)) { mismatches.add("MV File Number mismatch"); }

                    if (!mismatches.isEmpty()) {
                        match = false;
                        mismatchReason = String.join(", ", mismatches) + ".";
                    }
                } else {
                    match = false;
                    mismatchReason = "Vehicle details not found in VVS.";
                }

                if (od != null && match) {
                    // Owner matching logic removed as we don't save ownerName anymore
                }

                if (!match) {
                    savedRecord.setStatus("vehicle unmatch");
                    repository.save(savedRecord);
                    throw new RuntimeException("DCI validation failed: Input details do not match data in VVS system (" + mismatchReason + ").");
                }

                savedRecord.setVerificationId(vr.getId());
                savedRecord.setStatus("DOCUMENTS_VERIFIED");
                savedRecord.setCurrentStep(3);
                repository.save(savedRecord);
            }
        }

        // 2. Persist MVCC/MEC details if present
        if (payload.containsKey("mvcData") || payload.containsKey("mecData") || payload.containsKey("mvcNo") || payload.containsKey("remarks")) {
            MvcMecRequest mvcMecReq = mvcMecRequestRepository.findByCertificateRequestId(savedRecord.getId())
                    .orElse(new MvcMecRequest());
            mvcMecReq.setCertificateRequest(savedRecord);

            Map<?, ?> mvcData = (Map<?, ?>) payload.get("mvcData");
            Map<?, ?> mecData = (Map<?, ?>) payload.get("mecData");

            if (mvcData != null) {
                if (mvcData.get("mvcNo") != null) mvcMecReq.setMvcNo((String) mvcData.get("mvcNo"));
                if (mvcData.get("mvcIssueDate") != null) mvcMecReq.setMvcIssueDate((String) mvcData.get("mvcIssueDate"));
                if (mvcData.get("mvcStatus") != null) mvcMecReq.setMvcStatus((String) mvcData.get("mvcStatus"));
                if (mvcData.get("remarks") != null) mvcMecReq.setRemarks((String) mvcData.get("remarks"));
                if (mvcData.get("plateNo") != null) mvcMecReq.setPlateNumber((String) mvcData.get("plateNo"));
                else if (mvcData.get("plateNumber") != null) mvcMecReq.setPlateNumber((String) mvcData.get("plateNumber"));
                if (mvcData.get("mvFileNo") != null) mvcMecReq.setMvFileNumber((String) mvcData.get("mvFileNo"));
                else if (mvcData.get("mvFileNumber") != null) mvcMecReq.setMvFileNumber((String) mvcData.get("mvFileNumber"));
                if (mvcData.get("color") != null) mvcMecReq.setColor((String) mvcData.get("color"));
            }

            if (mecData != null) {
                if (mecData.get("engineNoStencilled") != null) mvcMecReq.setEngineNoStencilled((String) mecData.get("engineNoStencilled"));
                if (mecData.get("chassisNoStencilled") != null) mvcMecReq.setChassisNoStencilled((String) mecData.get("chassisNoStencilled"));
                if (mecData.get("hpgTechnician") != null) mvcMecReq.setHpgTechnician((String) mecData.get("hpgTechnician"));
            }

            if (payload.get("engineNoStencilled") != null) mvcMecReq.setEngineNoStencilled((String) payload.get("engineNoStencilled"));
            if (payload.get("chassisNoStencilled") != null) mvcMecReq.setChassisNoStencilled((String) payload.get("chassisNoStencilled"));
            if (payload.get("hpgTechnician") != null) mvcMecReq.setHpgTechnician((String) payload.get("hpgTechnician"));

            mvcMecRequestRepository.save(mvcMecReq);

            // Step 5 Validation Check!
            if ("MVC_MEC_VALIDATED".equals(payload.get("status"))) {
                String mvcEngine = mvcData != null && mvcData.get("engineNo") != null ? ((String) mvcData.get("engineNo")).trim().toUpperCase() : "";
                String mvcChassis = mvcData != null && mvcData.get("chassisNo") != null ? ((String) mvcData.get("chassisNo")).trim().toUpperCase() : "";
                String mvcPlate = mvcMecReq.getPlateNumber() != null ? mvcMecReq.getPlateNumber().trim().toUpperCase() : "";
                String mvcColor = mvcMecReq.getColor() != null ? mvcMecReq.getColor().trim().toUpperCase() : "";

                String mecEngine = mvcMecReq.getEngineNoStencilled() != null ? mvcMecReq.getEngineNoStencilled().trim().toUpperCase() : "";
                String mecChassis = mvcMecReq.getChassisNoStencilled() != null ? mvcMecReq.getChassisNoStencilled().trim().toUpperCase() : "";
                String mecPlate = mecData != null && mecData.get("plateNo") != null ? ((String) mecData.get("plateNo")).trim().toUpperCase() : "";
                String mecColor = mecData != null && mecData.get("color") != null ? ((String) mecData.get("color")).trim().toUpperCase() : "";

                OrCrRequest orCr = orCrRequestRepository.findByCertificateRequestId(savedRecord.getId()).orElse(null);

                if (orCr != null) {
                    String vEngine = orCr.getEngineNumber() != null ? orCr.getEngineNumber().trim().toUpperCase() : "";
                    String vChassis = orCr.getChassisNumber() != null ? orCr.getChassisNumber().trim().toUpperCase() : "";
                    String vPlate = orCr.getPlateNumber() != null ? orCr.getPlateNumber().trim().toUpperCase() : "";

                    if (!mvcEngine.equalsIgnoreCase(mecEngine)) {
                        throw new RuntimeException("DCI validation failed: Engine numbers do not match between MVCC and MEC.");
                    }
                    if (!mvcChassis.equalsIgnoreCase(mecChassis)) {
                        throw new RuntimeException("DCI validation failed: Chassis numbers do not match between MVCC and MEC.");
                    }
                    if (!mvcPlate.equalsIgnoreCase(mecPlate)) {
                        throw new RuntimeException("DCI validation failed: Plate numbers do not match between MVCC and MEC.");
                    }
                    if (!mvcColor.equalsIgnoreCase(mecColor)) {
                        throw new RuntimeException("DCI validation failed: Colors do not match between MVCC and MEC.");
                    }
                    if (!mvcEngine.equalsIgnoreCase(vEngine)) {
                        throw new RuntimeException("DCI validation failed: Engine number does not match verified OR/CR details.");
                    }
                    if (!mvcChassis.equalsIgnoreCase(vChassis)) {
                        throw new RuntimeException("DCI validation failed: Chassis number does not match verified OR/CR details.");
                    }
                    if (!mvcPlate.equalsIgnoreCase(vPlate)) {
                        throw new RuntimeException("DCI validation failed: Plate number does not match verified OR/CR details.");
                    }
                }
            }
        }

        // Voucher redemption
        if ("CERTIFICATE_ISSUED".equals(payload.get("status"))) {
            String finalVoucherCode = savedRecord.getVoucherCode();
            String finalCertNo = savedRecord.getCertificateNo() != null ? savedRecord.getCertificateNo() : (String) payload.get("certificateNo");
            if (finalVoucherCode != null && !finalVoucherCode.isBlank()) {
                if (finalCertNo == null || finalCertNo.isBlank()) {
                    finalCertNo = "DCI-PENDING-" + savedRecord.getId();
                }
                try {
                    Optional<Voucher> vOpt = voucherRepository.findByVoucherCode(finalVoucherCode);
                    if (vOpt.isPresent() && "AVAILABLE".equalsIgnoreCase(vOpt.get().getStatus())) {
                        voucherService.redeemVoucherByCode(finalVoucherCode, finalCertNo);
                    }
                } catch (Exception e) {
                    // Log the error but don't fail the save operation
                }
            }
        }

        return savedRecord;
    }

    public Map<String, Object> getRequestPayload(CertificateRequest record) {
        Map<String, Object> map = new java.util.HashMap<>();
        
        map.put("id", record.getId());
        if (record.getUser() != null) {
            map.put("userId", record.getUser().getId());
        }
        if (record.getCertificateNo() != null) {
            map.put("certificateNo", record.getCertificateNo());
            map.put("clearanceReferenceNo", record.getCertificateNo());
        }
        if (record.getVoucherCode() != null) {
            map.put("voucherCode", record.getVoucherCode());
            map.put("voucherReferenceNo", record.getVoucherCode());
        }
        if (record.getVoucher() != null) {
            map.put("voucherId", record.getVoucher().getId());
        }
        if (record.getStatus() != null) {
            map.put("status", record.getStatus());
        }
        if (record.getCurrentStep() != null) {
            map.put("currentStep", record.getCurrentStep());
        }

        // 1. Populate OR/CR details if present
        OrCrRequest orCr = orCrRequestRepository.findByCertificateRequestId(record.getId()).orElse(null);
        if (orCr != null) {
            map.put("vehicleOption", orCr.getVehicleOption());
            map.put("plateNumber", orCr.getPlateNumber());

            Map<String, Object> vehicleMap = new java.util.HashMap<>();
            vehicleMap.put("plateNumber", orCr.getPlateNumber() != null ? orCr.getPlateNumber() : "");
            vehicleMap.put("mvFileNumber", orCr.getMvFileNumber() != null ? orCr.getMvFileNumber() : "");
            vehicleMap.put("engineNumber", orCr.getEngineNumber() != null ? orCr.getEngineNumber() : "");
            vehicleMap.put("chassisNumber", orCr.getChassisNumber() != null ? orCr.getChassisNumber() : "");

            map.put("orCr", vehicleMap);
            map.put("crCr", vehicleMap);
        }

        // 2. Populate MVCC/MEC details if present
        MvcMecRequest mvcMec = mvcMecRequestRepository.findByCertificateRequestId(record.getId()).orElse(null);
        if (mvcMec != null) {
            Map<String, Object> mvcMap = new java.util.HashMap<>();
            mvcMap.put("mvcNo", mvcMec.getMvcNo() != null ? mvcMec.getMvcNo() : "");
            mvcMap.put("mvcIssueDate", mvcMec.getMvcIssueDate() != null ? mvcMec.getMvcIssueDate() : "");
            mvcMap.put("mvcStatus", mvcMec.getMvcStatus() != null ? mvcMec.getMvcStatus() : "");
            mvcMap.put("remarks", mvcMec.getRemarks() != null ? mvcMec.getRemarks() : "");
            mvcMap.put("plateNumber", mvcMec.getPlateNumber() != null ? mvcMec.getPlateNumber() : "");
            mvcMap.put("mvFileNumber", mvcMec.getMvFileNumber() != null ? mvcMec.getMvFileNumber() : "");
            mvcMap.put("color", mvcMec.getColor() != null ? mvcMec.getColor() : "");
            mvcMap.put("engineNo", mvcMec.getEngineNoStencilled() != null ? mvcMec.getEngineNoStencilled() : "");
            mvcMap.put("chassisNo", mvcMec.getChassisNoStencilled() != null ? mvcMec.getChassisNoStencilled() : "");

            Map<String, Object> mecMap = new java.util.HashMap<>();
            mecMap.put("engineNoStencilled", mvcMec.getEngineNoStencilled() != null ? mvcMec.getEngineNoStencilled() : "");
            mecMap.put("chassisNoStencilled", mvcMec.getChassisNoStencilled() != null ? mvcMec.getChassisNoStencilled() : "");
            mecMap.put("hpgTechnician", mvcMec.getHpgTechnician() != null ? mvcMec.getHpgTechnician() : "");

            map.put("mvcData", mvcMap);
            map.put("mecData", mecMap);
        }

        // 3. Keep the validationId VVS logic if matching verification exists
        Long verificationId = record.getVerificationId();
        if (verificationId != null) {
            map.put("verificationId", verificationId);
            VerificationRequest verificationRequest = verificationRequestRepo.findById(verificationId).orElse(null);
            if (verificationRequest != null) {
                map.put("verificationStatus", verificationRequest.getVerificationStatus().toString());
            }
        }

        return map;
    }

    public Optional<Map<String, Object>> getVerificationDetailsByVoucherCode(String voucherCode) {
        Optional<Voucher> voucherOpt = voucherRepository.findByVoucherCode(voucherCode);
        if (voucherOpt.isEmpty()) {
            return Optional.empty();
        }
        Voucher voucher = voucherOpt.get();
        
        Optional<CertificateRequest> requestOpt = repository.findFirstByVoucherCodeOrderByIdDesc(voucherCode);
        if (requestOpt.isEmpty()) {
            return Optional.empty();
        }
        CertificateRequest record = requestOpt.get();
        
        OrCrRequest orCr = orCrRequestRepository.findByCertificateRequestId(record.getId()).orElse(null);
        if (orCr == null) {
            return Optional.empty();
        }
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", record.getId());
        response.put("voucherCode", voucherCode);
        response.put("status", record.getStatus());
        
        Map<String, Object> vehicleData = new java.util.HashMap<>();
        vehicleData.put("plateNumber", orCr.getPlateNumber() != null ? orCr.getPlateNumber() : "");
        vehicleData.put("mvFileNumber", orCr.getMvFileNumber() != null ? orCr.getMvFileNumber() : "");
        vehicleData.put("engineNumber", orCr.getEngineNumber() != null ? orCr.getEngineNumber() : "");
        vehicleData.put("chassisNumber", orCr.getChassisNumber() != null ? orCr.getChassisNumber() : "");
        
        response.put("vehicleData", vehicleData);
        return Optional.of(response);
    }

    @Transactional
    public CertificateRequest verifyRequestByVoucherCode(String voucherCode) {
        CertificateRequest record = repository.findFirstByVoucherCodeOrderByIdDesc(voucherCode)
                .orElseThrow(() -> new RuntimeException("Certificate request not found for voucher code: " + voucherCode));

        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher not found for voucher code: " + voucherCode));

        if (Boolean.TRUE.equals(voucher.getHpgVerified()) || 
            "HPG_VERIFIED".equals(record.getStatus()) || 
            "MVC_MEC_VALIDATED".equals(record.getStatus()) || 
            "CERTIFICATE_ISSUED".equals(record.getStatus())) {
            throw new RuntimeException("This request has already been verified by HPG.");
        }

        record.setStatus("HPG_VERIFIED");

        if (record.getUser() != null) {
            User.UserRole userRole = record.getUser().getRole();
            if (User.UserRole.CITIZEN.equals(userRole)) {
                record.setCurrentStep(5);
            } else if (User.UserRole.AGENT_FIXER.equals(userRole)) {
                record.setCurrentStep(4);
            }
        }

        voucher.setHpgVerified(true);
        voucherRepository.save(voucher);

        return repository.save(record);
    }
}
