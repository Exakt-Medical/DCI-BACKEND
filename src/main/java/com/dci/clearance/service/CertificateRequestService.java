package com.dci.clearance.service;

import com.dci.clearance.entity.User;
import com.dci.clearance.entity.CertificateRequest;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.repository.CertificateRequestRepository;
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
        if (payload.get("voucherCode") != null) {
            record.setVoucherCode((String) payload.get("voucherCode"));
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            record.setPayloadJson(mapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload");
        }

        return repository.save(record);
    }
}
