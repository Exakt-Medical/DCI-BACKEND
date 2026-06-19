package com.dci.clearance.service;

import com.dci.clearance.entity.User;
import com.dci.clearance.entity.CertificateRequest;
import com.dci.clearance.entity.Voucher;
import com.dci.clearance.entity.VerificationVehicleDetails;
import com.dci.clearance.entity.VerificationOwnerDetails;
import com.dci.clearance.entity.VerificationRequest;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.repository.CertificateRequestRepository;
import com.dci.clearance.repository.VoucherRepository;
import com.dci.clearance.repository.VerificationVehicleDetailsRepository;
import com.dci.clearance.repository.VerificationOwnerDetailsRepository;
import com.dci.clearance.repository.VerificationRequestRepository;
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
        if (payload.get("plateNumber") != null) {
            record.setPlateNumber((String) payload.get("plateNumber"));
        } else if (payload.get("orCr") instanceof Map) {
            Map<?, ?> orCr = (Map<?, ?>) payload.get("orCr");
            if (orCr.get("plateNumber") != null) {
                record.setPlateNumber((String) orCr.get("plateNumber"));
            }
        }
        if (payload.get("certificateNo") != null) {
            record.setCertificateNo((String) payload.get("certificateNo"));
        }
        if ("CERTIFICATE_ISSUED".equals(payload.get("status"))) {
            if (record.getCertificateNo() == null || record.getCertificateNo().isBlank() || record.getCertificateNo().startsWith("DCI-CERT-17") || record.getCertificateNo().startsWith("DCI-CERT-18")) {
                String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                String certNo = "DCI-CERT-" + dateStr + "-" + record.getId();
                record.setCertificateNo(certNo);
            }
        }
        if (payload.get("voucherCode") != null) {
            record.setVoucherCode((String) payload.get("voucherCode"));
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
        
        if (verificationId == null && record != null) {
            verificationId = record.getVerificationId();
            if (verificationId == null && record.getPayloadJson() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> oldPayload = mapper.readValue(record.getPayloadJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
                    Object vIdObj = oldPayload.get("verificationId");
                    if (vIdObj instanceof Number) {
                        verificationId = ((Number) vIdObj).longValue();
                    } else if (vIdObj instanceof String && !((String) vIdObj).isEmpty()) {
                        verificationId = Long.parseLong((String) vIdObj);
                    }
                } catch (Exception ignored) {}
            }
        }
        
        String voucherCode = null;
        Object vCodeObj = payload.get("voucherCode");
        if (vCodeObj instanceof String && !((String) vCodeObj).isEmpty()) {
            voucherCode = (String) vCodeObj;
        }

        if (verificationId != null && voucherCode != null) {
            try {
                Optional<Voucher> voucherOpt = voucherRepository.findByVoucherCode(voucherCode);
                Optional<VerificationVehicleDetails> vehicleDetailsOpt = vehicleDetailsRepo.findByVerificationId(verificationId);
                if (voucherOpt.isPresent() && vehicleDetailsOpt.isPresent()) {
                    VerificationVehicleDetails vd = vehicleDetailsOpt.get();
                    vd.setVoucherId(voucherOpt.get().getId());
                    vehicleDetailsRepo.save(vd);
                }
            } catch (Exception ignored) {}
        }

        if (verificationId != null) {
            record.setVerificationId(verificationId);
        }

        if ("MVC_MEC_VALIDATED".equals(payload.get("status"))) {
            if (verificationId != null) {
                Optional<VerificationRequest> vrOpt = verificationRequestRepo.findById(verificationId);
                if (vrOpt.isEmpty() || vrOpt.get().getVerificationStatus() != VerificationRequest.VerificationStatus.VERIFIED) {
                    throw new RuntimeException("DCI validation failed: Verification status in database must be VERIFIED.");
                }
            } else {
                throw new RuntimeException("DCI validation failed: Missing verification ID.");
            }
        }

        Map<String, Object> sanitizedPayload = new java.util.HashMap<>(payload);
        sanitizedPayload.remove("orCr");
        sanitizedPayload.remove("crCr");
        sanitizedPayload.remove("mvcData");
        sanitizedPayload.remove("mecData");
        sanitizedPayload.remove("orPreview");
        sanitizedPayload.remove("crPreview");
        sanitizedPayload.remove("mvcPreview");
        sanitizedPayload.remove("mecPreview");
        sanitizedPayload.remove("mvcFileName");
        sanitizedPayload.remove("mecFileName");

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            record.setPayloadJson(mapper.writeValueAsString(sanitizedPayload));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload");
        }

        CertificateRequest savedRecord = repository.save(record);

        if ("CERTIFICATE_ISSUED".equals(payload.get("status")) || "MVC_MEC_VALIDATED".equals(payload.get("status"))) {
            String finalVoucherCode = voucherCode != null ? voucherCode : savedRecord.getVoucherCode();
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
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> map;
        try {
            map = mapper.readValue(record.getPayloadJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
        } catch (Exception e) {
            map = new java.util.HashMap<>();
        }
        map.put("id", record.getId());
        if (record.getCertificateNo() != null) {
            map.put("certificateNo", record.getCertificateNo());
            map.put("clearanceReferenceNo", record.getCertificateNo());
        }
        if (record.getVoucherCode() != null) {
            map.put("voucherCode", record.getVoucherCode());
            map.put("voucherReferenceNo", record.getVoucherCode());
        }
        if (record.getStatus() != null) {
            map.put("status", record.getStatus());
        }
        if (record.getCurrentStep() != null) {
            map.put("currentStep", record.getCurrentStep());
        }
        if (record.getPlateNumber() != null) {
            map.put("plateNumber", record.getPlateNumber());
        }
        
        Long verificationId = record.getVerificationId();
        if (verificationId != null) {
            map.put("verificationId", verificationId);
            VerificationVehicleDetails vehicleDetails = vehicleDetailsRepo.findByVerificationId(verificationId).orElse(null);
            VerificationOwnerDetails ownerDetails = ownerDetailsRepo.findByVerificationId(verificationId).orElse(null);
            VerificationRequest verificationRequest = verificationRequestRepo.findById(verificationId).orElse(null);
            
            if (verificationRequest != null) {
                map.put("verificationStatus", verificationRequest.getVerificationStatus().toString());
            }
            
            if (vehicleDetails != null) {
                Map<String, Object> vehicleMap = new java.util.HashMap<>();
                if (verificationRequest != null) {
                    vehicleMap.put("plateNumber", verificationRequest.getPlateNumber());
                    vehicleMap.put("mvFileNumber", verificationRequest.getMvFileNumber());
                    vehicleMap.put("engineNumber", verificationRequest.getEngineNumber());
                    vehicleMap.put("chassisNumber", verificationRequest.getChassisNumber());
                    vehicleMap.put("verificationStatus", verificationRequest.getVerificationStatus().toString());
                } else {
                    vehicleMap.put("plateNumber", "");
                    vehicleMap.put("mvFileNumber", "");
                    vehicleMap.put("engineNumber", "");
                    vehicleMap.put("chassisNumber", "");
                    vehicleMap.put("verificationStatus", "");
                }
                vehicleMap.put("classification", vehicleDetails.getClassification());
                vehicleMap.put("vehicleType", vehicleDetails.getBodyType());
                vehicleMap.put("fuelType", vehicleDetails.getDenomination());
                vehicleMap.put("make", vehicleDetails.getMake());
                vehicleMap.put("series", vehicleDetails.getSeries());
                vehicleMap.put("yearModel", vehicleDetails.getYearModel());
                vehicleMap.put("color", vehicleDetails.getColor());
                
                if (ownerDetails != null) {
                    String fullName = String.format("%s %s %s", 
                        ownerDetails.getFirstName() != null ? ownerDetails.getFirstName() : "",
                        ownerDetails.getMiddleName() != null ? ownerDetails.getMiddleName() : "",
                        ownerDetails.getLastName() != null ? ownerDetails.getLastName() : ""
                    ).replaceAll("\\s+", " ").trim();
                    vehicleMap.put("ownerName", fullName);
                    
                    String fullAddress = String.format("%s %s %s %s %s %s",
                        ownerDetails.getHouseBldgNo() != null ? ownerDetails.getHouseBldgNo() : "",
                        ownerDetails.getStreetName() != null ? ownerDetails.getStreetName() : "",
                        ownerDetails.getBarangay() != null ? ownerDetails.getBarangay() : "",
                        ownerDetails.getMunicipality() != null ? ownerDetails.getMunicipality() : "",
                        ownerDetails.getProvince() != null ? ownerDetails.getProvince() : "",
                        ownerDetails.getZipCode() != null ? ownerDetails.getZipCode() : ""
                    ).replaceAll("\\s+", " ").trim();
                    vehicleMap.put("ownerAddress", fullAddress);
                } else {
                    vehicleMap.put("ownerName", "");
                    vehicleMap.put("ownerAddress", "");
                }
                
                map.put("orCr", vehicleMap);
                map.put("crCr", vehicleMap);
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
        
        VerificationVehicleDetails vehicleDetails = vehicleDetailsRepo.findByVoucherId(voucher.getId()).orElse(null);
        VerificationOwnerDetails ownerDetails = null;
        Long certificateRequestId = null;
        String status = "PENDING_HPG";
        
        if (vehicleDetails == null) {
            Optional<CertificateRequest> requestOpt = repository.findFirstByVoucherCodeOrderByIdDesc(voucherCode);
            if (requestOpt.isPresent()) {
                CertificateRequest record = requestOpt.get();
                certificateRequestId = record.getId();
                status = record.getStatus();
                
                Long verificationId = record.getVerificationId();
                if (verificationId == null && record.getPayloadJson() != null) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, Object> payload = mapper.readValue(record.getPayloadJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
                        Object vIdObj = payload.get("verificationId");
                        if (vIdObj instanceof Number) {
                            verificationId = ((Number) vIdObj).longValue();
                        } else if (vIdObj instanceof String && !((String) vIdObj).isEmpty()) {
                            verificationId = Long.parseLong((String) vIdObj);
                        }
                    } catch (Exception ignored) {}
                }
                if (verificationId != null) {
                    vehicleDetails = vehicleDetailsRepo.findByVerificationId(verificationId).orElse(null);
                }
            }
        } else {
            ownerDetails = ownerDetailsRepo.findByVerificationId(vehicleDetails.getVerificationId()).orElse(null);
            Optional<CertificateRequest> requestOpt = repository.findFirstByVoucherCodeOrderByIdDesc(voucherCode);
            if (requestOpt.isPresent()) {
                certificateRequestId = requestOpt.get().getId();
                status = requestOpt.get().getStatus();
            }
        }
        
        if (vehicleDetails != null) {
            if (ownerDetails == null) {
                ownerDetails = ownerDetailsRepo.findByVerificationId(vehicleDetails.getVerificationId()).orElse(null);
            }
            VerificationRequest verificationRequest = verificationRequestRepo.findById(vehicleDetails.getVerificationId()).orElse(null);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", certificateRequestId);
            response.put("voucherCode", voucherCode);
            response.put("status", status);
            
            Map<String, Object> vehicleData = new java.util.HashMap<>();
            if (verificationRequest != null) {
                vehicleData.put("plateNumber", verificationRequest.getPlateNumber());
                vehicleData.put("mvFileNumber", verificationRequest.getMvFileNumber());
                vehicleData.put("engineNumber", verificationRequest.getEngineNumber());
                vehicleData.put("chassisNumber", verificationRequest.getChassisNumber());
                vehicleData.put("verificationStatus", verificationRequest.getVerificationStatus().toString());
            } else {
                vehicleData.put("plateNumber", "");
                vehicleData.put("mvFileNumber", "");
                vehicleData.put("engineNumber", "");
                vehicleData.put("chassisNumber", "");
                vehicleData.put("verificationStatus", "");
            }
            vehicleData.put("make", vehicleDetails.getMake());
            vehicleData.put("series", vehicleDetails.getSeries());
            vehicleData.put("yearModel", vehicleDetails.getYearModel());
            vehicleData.put("color", vehicleDetails.getColor());
            
            if (ownerDetails != null) {
                String fullName = String.format("%s %s %s", 
                    ownerDetails.getFirstName() != null ? ownerDetails.getFirstName() : "",
                    ownerDetails.getMiddleName() != null ? ownerDetails.getMiddleName() : "",
                    ownerDetails.getLastName() != null ? ownerDetails.getLastName() : ""
                ).replaceAll("\\s+", " ").trim();
                vehicleData.put("ownerName", fullName);
            } else {
                vehicleData.put("ownerName", "");
            }
            
            response.put("vehicleData", vehicleData);
            return Optional.of(response);
        }
        
        return Optional.empty();
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

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(record.getPayloadJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
            payload.put("status", "HPG_VERIFIED");
            payload.put("hpgVerified", true);
            
            Object role = payload.get("role");
            if ("citizen".equals(role)) {
                payload.put("currentStep", 5);
                record.setCurrentStep(5);
            } else if ("agent_fixer".equals(role)) {
                payload.put("currentStep", 3);
                record.setCurrentStep(3);
                payload.put("hpgStatus", "HPG_APPROVED");
            }
            record.setPayloadJson(mapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update request payload: " + e.getMessage());
        }

        voucher.setHpgVerified(true);
        voucherRepository.save(voucher);

        return repository.save(record);
    }
}
