package com.dci.clearance.controller;

import com.dci.clearance.entity.CertificateRequest;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.service.CertificateRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificate-requests")
@RequiredArgsConstructor
public class CertificateRequestController {

    private final CertificateRequestService service;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        Long userId = getUserId(auth);
        List<CertificateRequest> records = service.getMyRequests(userId);
        
        List<Map<String, Object>> response = records.stream()
                .map(service::getRequestPayload)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> upsertRequest(@RequestBody Map<String, Object> payload, Authentication auth) {
        Long userId = getUserId(auth);
        try {
            CertificateRequest saved = service.upsertRequest(userId, payload);
            return ResponseEntity.ok(Map.of("message", "Saved successfully", "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/by-voucher/{voucherCode}")
    public ResponseEntity<?> getRequestByVoucher(@PathVariable String voucherCode) {
        Optional<Map<String, Object>> opt = service.getVerificationDetailsByVoucherCode(voucherCode);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @PostMapping("/by-voucher/{voucherCode}/verify")
    public ResponseEntity<?> verifyRequestByVoucher(@PathVariable String voucherCode) {
        try {
            CertificateRequest saved = service.verifyRequestByVoucherCode(voucherCode);
            return ResponseEntity.ok(Map.of("message", "Voucher verified by HPG successfully", "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
