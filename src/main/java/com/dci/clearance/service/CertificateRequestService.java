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

    public Map<String, Object> getMyRequestsPaginated(Long userId, int page, int size, String search, String activeFilter) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dateUpdated"));
        
        org.springframework.data.domain.Page<CertificateRequest> pagedResult = repository.findPaginatedAndFiltered(
            userId, 
            activeFilter == null || activeFilter.isEmpty() ? "all" : activeFilter, 
            search, 
            pageable
        );
        
        List<Map<String, Object>> payloads = getRequestPayloads(pagedResult.getContent());
        
        long allCount = repository.countByUserId(userId);
        long completedCount = repository.countCompletedByUserId(userId);
        long voucherCount = repository.countVoucherInProgressByUserId(userId);
        long clearanceCount = repository.countClearanceAwaitingByUserId(userId);
        
        Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("all", allCount);
        counts.put("completed", completedCount);
        counts.put("voucher", voucherCount);
        counts.put("clearance", clearanceCount);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("content", payloads);
        response.put("totalPages", pagedResult.getTotalPages());
        response.put("totalElements", pagedResult.getTotalElements());
        response.put("counts", counts);
        
        return response;
    }

    public Optional<CertificateRequest> getRequestById(Long id) {
        return repository.findById(id);
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

        if (payload.get("transactionType") != null) {
            record.setVehicleTransactionType((String) payload.get("transactionType"));
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
                if (orMap.get("engineNumber") != null) orCrReq.setEngineNumber((String) orMap.get("engineNumber"));
                if (orMap.get("chassisNumber") != null) orCrReq.setChassisNumber((String) orMap.get("chassisNumber"));
                if (orMap.get("makeBrand") != null) orCrReq.setMakeBrand((String) orMap.get("makeBrand"));
                if (orMap.get("color") != null) orCrReq.setColor((String) orMap.get("color"));
                if (orMap.get("classification") != null) orCrReq.setClassification((String) orMap.get("classification"));
                if (orMap.get("series") != null) orCrReq.setSeries((String) orMap.get("series"));
                if (orMap.get("yearModel") != null) orCrReq.setYearModel((String) orMap.get("yearModel"));
                if (orMap.get("ownerName") != null) orCrReq.setOwnerName((String) orMap.get("ownerName"));
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
                if (crMap.get("makeBrand") != null) {
                    orCrReq.setMakeBrand((String) crMap.get("makeBrand"));
                }
                if (crMap.get("color") != null) {
                    orCrReq.setColor((String) crMap.get("color"));
                }
                if (crMap.get("classification") != null) {
                    orCrReq.setClassification((String) crMap.get("classification"));
                }
                if (crMap.get("series") != null) {
                    orCrReq.setSeries((String) crMap.get("series"));
                }
                if (crMap.get("yearModel") != null) {
                    orCrReq.setYearModel((String) crMap.get("yearModel"));
                }
                if (crMap.get("ownerName") != null) {
                    orCrReq.setOwnerName((String) crMap.get("ownerName"));
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
                    savedRecord.setStatus("VERIFICATION_FAILED");
                    repository.save(savedRecord);
                    throw new RuntimeException("DCI validation failed: No matching verified vehicle record found in VVS system.");
                }

                VerificationRequest vr = vvsOpt.get();

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

    public List<Map<String, Object>> getRequestPayloads(List<CertificateRequest> records) {
        if (records == null || records.isEmpty()) return java.util.Collections.emptyList();

        List<Long> requestIds = records.stream().map(CertificateRequest::getId).collect(java.util.stream.Collectors.toList());
        List<Long> verificationIds = records.stream().map(CertificateRequest::getVerificationId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());

        Map<Long, OrCrRequest> orCrMap = orCrRequestRepository.findByCertificateRequestIdIn(requestIds)
                .stream().collect(java.util.stream.Collectors.toMap(req -> req.getCertificateRequest().getId(), req -> req, (existing, replacement) -> existing));

        Map<Long, MvcMecRequest> mvcMecMap = mvcMecRequestRepository.findByCertificateRequestIdIn(requestIds)
                .stream().collect(java.util.stream.Collectors.toMap(req -> req.getCertificateRequest().getId(), req -> req, (existing, replacement) -> existing));

        final Map<Long, VerificationRequest> verificationMap = new java.util.HashMap<>();
        final Map<Long, VerificationVehicleDetails> vehicleDetailsMap = new java.util.HashMap<>();
        final Map<Long, VerificationOwnerDetails> ownerDetailsMap = new java.util.HashMap<>();

        if (!verificationIds.isEmpty()) {
            verificationMap.putAll(verificationRequestRepo.findAllById(verificationIds)
                    .stream().collect(java.util.stream.Collectors.toMap(VerificationRequest::getId, v -> v, (existing, replacement) -> existing)));
            vehicleDetailsMap.putAll(vehicleDetailsRepo.findByVerificationIdIn(verificationIds)
                    .stream().collect(java.util.stream.Collectors.toMap(VerificationVehicleDetails::getVerificationId, v -> v, (existing, replacement) -> existing)));
            ownerDetailsMap.putAll(ownerDetailsRepo.findByVerificationIdIn(verificationIds)
                    .stream().collect(java.util.stream.Collectors.toMap(VerificationOwnerDetails::getVerificationId, v -> v, (existing, replacement) -> existing)));
        }

        return records.stream().map(record -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", record.getId());
            if (record.getUser() != null) map.put("userId", record.getUser().getId());
            if (record.getCertificateNo() != null) {
                map.put("certificateNo", record.getCertificateNo());
                map.put("clearanceReferenceNo", record.getCertificateNo());
            }
            if (record.getVoucherCode() != null) {
                map.put("voucherCode", record.getVoucherCode());
                map.put("voucherReferenceNo", record.getVoucherCode());
            }
            if (record.getVoucher() != null) map.put("voucherId", record.getVoucher().getId());
            if (record.getStatus() != null) map.put("status", record.getStatus());
            if (record.getCurrentStep() != null) map.put("currentStep", record.getCurrentStep());
            if (record.getVehicleTransactionType() != null) map.put("transactionType", record.getVehicleTransactionType());
            if (record.getDateCreated() != null) map.put("dateCreated", record.getDateCreated().toString());
            if (record.getDateUpdated() != null) map.put("dateUpdated", record.getDateUpdated().toString());

            OrCrRequest orCr = orCrMap.get(record.getId());
            if (orCr != null) {
                map.put("vehicleOption", orCr.getVehicleOption());
                map.put("plateNumber", orCr.getPlateNumber());

                Map<String, Object> vehicleMap = new java.util.HashMap<>();
                vehicleMap.put("plateNumber", orCr.getPlateNumber() != null ? orCr.getPlateNumber() : "");
                vehicleMap.put("mvFileNumber", orCr.getMvFileNumber() != null ? orCr.getMvFileNumber() : "");
                vehicleMap.put("engineNumber", orCr.getEngineNumber() != null ? orCr.getEngineNumber() : "");
                vehicleMap.put("chassisNumber", orCr.getChassisNumber() != null ? orCr.getChassisNumber() : "");
                vehicleMap.put("makeBrand", orCr.getMakeBrand() != null ? orCr.getMakeBrand() : "");
                vehicleMap.put("color", orCr.getColor() != null ? orCr.getColor() : "");
                vehicleMap.put("classification", orCr.getClassification() != null ? orCr.getClassification() : "");
                vehicleMap.put("series", orCr.getSeries() != null ? orCr.getSeries() : "");
                vehicleMap.put("yearModel", orCr.getYearModel() != null ? orCr.getYearModel() : "");
                vehicleMap.put("ownerName", orCr.getOwnerName() != null ? orCr.getOwnerName() : "");

                map.put("orCr", vehicleMap);
                map.put("crCr", vehicleMap);
            }

            MvcMecRequest mvcMec = mvcMecMap.get(record.getId());
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

            Long verificationId = record.getVerificationId();
            if (verificationId != null) {
                map.put("verificationId", verificationId);
                VerificationRequest verificationRequest = verificationMap.get(verificationId);
                if (verificationRequest != null) {
                    map.put("verificationStatus", verificationRequest.getVerificationStatus().toString());

                    VerificationVehicleDetails vehicleDetails = vehicleDetailsMap.get(verificationId);
                    if (vehicleDetails != null) {
                        Map<String, Object> vMap = new java.util.HashMap<>();
                        vMap.put("make", vehicleDetails.getMake());
                        vMap.put("series", vehicleDetails.getSeries());
                        vMap.put("color", vehicleDetails.getColor());
                        vMap.put("yearModel", vehicleDetails.getYearModel());
                        vMap.put("classification", vehicleDetails.getClassification());
                        vMap.put("bodyType", vehicleDetails.getBodyType());
                        vMap.put("denomination", vehicleDetails.getDenomination());
                        vMap.put("lastRegistrationDate", vehicleDetails.getLastRegistrationDate());
                        vMap.put("verificationId", verificationId);
                        vMap.put("mvFileNumber", verificationRequest.getMvFileNumber());
                        vMap.put("plateNumber", verificationRequest.getPlateNumber());
                        vMap.put("chassisNumber", verificationRequest.getChassisNumber());
                        vMap.put("engineNumber", verificationRequest.getEngineNumber());
                        map.put("vvsVehicleDetails", vMap);
                    }

                    VerificationOwnerDetails ownerDetails = ownerDetailsMap.get(verificationId);
                    if (ownerDetails != null) {
                        String firstName = ownerDetails.getFirstName() != null ? ownerDetails.getFirstName() : "";
                        String middleName = ownerDetails.getMiddleName() != null ? ownerDetails.getMiddleName() : "";
                        String lastName = ownerDetails.getLastName() != null ? ownerDetails.getLastName() : "";
                        String fullName = String.join(" ", java.util.Arrays.asList(firstName, middleName, lastName)).replaceAll("\\s+", " ").trim();
                        if (fullName.isEmpty() && ownerDetails.getOrganization() != null) {
                            fullName = ownerDetails.getOrganization();
                        }
                        map.put("vvsOwnerName", fullName.isEmpty() ? "Unknown Owner" : fullName);
                    }
                }
            }

            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    public Map<String, Object> getRequestPayload(CertificateRequest record) {
        return getRequestPayloads(java.util.Collections.singletonList(record)).get(0);
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
        
        Long verificationId = record.getVerificationId();
        VerificationRequest vvsReq = null;
        if (verificationId != null) {
            vvsReq = verificationRequestRepo.findById(verificationId).orElse(null);
        }
        
        if (vvsReq != null) {
            vehicleData.put("plateNumber", vvsReq.getPlateNumber() != null ? vvsReq.getPlateNumber() : "");
            vehicleData.put("mvFileNumber", vvsReq.getMvFileNumber() != null ? vvsReq.getMvFileNumber() : "");
            vehicleData.put("engineNumber", vvsReq.getEngineNumber() != null ? vvsReq.getEngineNumber() : "");
            vehicleData.put("chassisNumber", vvsReq.getChassisNumber() != null ? vvsReq.getChassisNumber() : "");
        } else {
            vehicleData.put("plateNumber", orCr.getPlateNumber() != null ? orCr.getPlateNumber() : "");
            vehicleData.put("mvFileNumber", orCr.getMvFileNumber() != null ? orCr.getMvFileNumber() : "");
            vehicleData.put("engineNumber", orCr.getEngineNumber() != null ? orCr.getEngineNumber() : "");
            vehicleData.put("chassisNumber", orCr.getChassisNumber() != null ? orCr.getChassisNumber() : "");
        }
        vehicleData.put("verificationStatus", record.getStatus());

        if (verificationId != null) {
            VerificationVehicleDetails vehicleDetails = vehicleDetailsRepo.findByVerificationId(verificationId).orElse(null);
            if (vehicleDetails != null) {
                vehicleData.put("make", vehicleDetails.getMake());
                vehicleData.put("series", vehicleDetails.getSeries());
                vehicleData.put("color", vehicleDetails.getColor());
                vehicleData.put("yearModel", vehicleDetails.getYearModel());
                vehicleData.put("classification", vehicleDetails.getClassification());
                vehicleData.put("bodyType", vehicleDetails.getBodyType());
                vehicleData.put("denomination", vehicleDetails.getDenomination());
                vehicleData.put("lastRegistrationDate", vehicleDetails.getLastRegistrationDate());
            }

            VerificationOwnerDetails ownerDetails = ownerDetailsRepo.findByVerificationId(verificationId).orElse(null);
            if (ownerDetails != null) {
                String firstName = ownerDetails.getFirstName() != null ? ownerDetails.getFirstName() : "";
                String middleName = ownerDetails.getMiddleName() != null ? ownerDetails.getMiddleName() : "";
                String lastName = ownerDetails.getLastName() != null ? ownerDetails.getLastName() : "";
                String fullName = String.join(" ", java.util.Arrays.asList(firstName, middleName, lastName)).replaceAll("\\s+", " ").trim();
                if (fullName.isEmpty() && ownerDetails.getOrganization() != null) {
                    fullName = ownerDetails.getOrganization();
                }
                vehicleData.put("ownerName", fullName.isEmpty() ? "Unknown Owner" : fullName);
            }
        }
        
        response.put("vehicleData", vehicleData);
        return Optional.of(response);
    }

    @Transactional
    public CertificateRequest verifyRequestByVoucherCode(String voucherCode, User verifier, Map<String, Object> mvcData, Map<String, Object> mecData) {
        CertificateRequest record = repository.findFirstByVoucherCodeOrderByIdDesc(voucherCode)
                .orElseThrow(() -> new RuntimeException("Certificate request not found for voucher code: " + voucherCode));

        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher not found for voucher code: " + voucherCode));

        boolean isDoingHpgVerification = false;

        if (verifier.getRole() == User.UserRole.HPG) {
            isDoingHpgVerification = true;
        } else if (verifier.getRole() == User.UserRole.DCI) {
            if (!"HPG_VERIFIED".equals(record.getStatus()) && 
                !"MVC_MEC_VALIDATED".equals(record.getStatus()) && 
                !"CERTIFICATE_ISSUED".equals(record.getStatus())) {
                isDoingHpgVerification = true;
            }
        } else {
            throw new RuntimeException("Unauthorized role for verification");
        }

        if (isDoingHpgVerification) {
            if (Boolean.TRUE.equals(voucher.getHpgVerified()) || 
                "HPG_VERIFIED".equals(record.getStatus()) || 
                "MVC_MEC_VALIDATED".equals(record.getStatus()) || 
                "CERTIFICATE_ISSUED".equals(record.getStatus())) {
                throw new RuntimeException("This request has already been verified by HPG.");
            }
            record.setStatus("HPG_VERIFIED");
            voucher.setHpgVerified(true);
            voucherRepository.save(voucher);
        } else {
            if ("MVC_MEC_VALIDATED".equals(record.getStatus()) || 
                "CERTIFICATE_ISSUED".equals(record.getStatus())) {
                throw new RuntimeException("This request has already been validated by DCI.");
            }
            if (!"HPG_VERIFIED".equals(record.getStatus())) {
                throw new RuntimeException("Request must be verified by HPG before DCI validation.");
            }

            record.setStatus("MVC_MEC_VALIDATED");
            if (mvcData != null || mecData != null) {
                MvcMecRequest mvcMecReq = mvcMecRequestRepository.findByCertificateRequestId(record.getId())
                        .orElse(new MvcMecRequest());
                mvcMecReq.setCertificateRequest(record);

                if (mvcData != null) {
                    if (mvcData.get("mvcNo") != null) mvcMecReq.setMvcNo((String) mvcData.get("mvcNo"));
                    if (mvcData.get("issueDate") != null) mvcMecReq.setMvcIssueDate((String) mvcData.get("issueDate"));
                    if (mvcData.get("mvFileNo") != null) mvcMecReq.setMvFileNumber((String) mvcData.get("mvFileNo"));
                    if (mvcData.get("engineNo") != null) mvcMecReq.setEngineNoStencilled((String) mvcData.get("engineNo"));
                    if (mvcData.get("chassisNo") != null) mvcMecReq.setChassisNoStencilled((String) mvcData.get("chassisNo"));
                    if (mvcData.get("plateNo") != null) mvcMecReq.setPlateNumber((String) mvcData.get("plateNo"));
                    if (mvcData.get("color") != null) mvcMecReq.setColor((String) mvcData.get("color"));
                }
                
                if (mecData != null) {
                    if (mecData.get("engineNoStencilled") != null) mvcMecReq.setEngineNoStencilled((String) mecData.get("engineNoStencilled"));
                    if (mecData.get("chassisNoStencilled") != null) mvcMecReq.setChassisNoStencilled((String) mecData.get("chassisNoStencilled"));
                }

                mvcMecRequestRepository.save(mvcMecReq);
            }
        }

        if (record.getUser() != null) {
            User.UserRole userRole = record.getUser().getRole();
            if (User.UserRole.CITIZEN.equals(userRole)) {
                record.setCurrentStep(5);
            } else if (User.UserRole.AGENT_FIXER.equals(userRole)) {
                record.setCurrentStep(4);
            }
        }



        return repository.save(record);
    }
}
