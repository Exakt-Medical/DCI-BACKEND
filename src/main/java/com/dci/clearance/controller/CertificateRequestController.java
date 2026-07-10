package com.dci.clearance.controller;

import com.dci.clearance.entity.CertificateRequest;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.service.CertificateRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

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
        List<Map<String, Object>> response = service.getRequestPayloads(records);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable Long id, Authentication auth) {
        Optional<CertificateRequest> opt = service.getRequestById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CertificateRequest record = opt.get();
        // Option 1: allow anyone who is authenticated to poll (for simplicity, or restrict by userId)
        return ResponseEntity.ok(service.getRequestPayload(record));
    }

    @PostMapping
    public ResponseEntity<?> upsertRequest(@RequestBody Map<String, Object> payload, Authentication auth) {
        Long userId = getUserId(auth);
        try {
            CertificateRequest saved = service.upsertRequest(userId, payload);
            return ResponseEntity.ok(Map.of(
                "message", "Saved successfully",
                "id", saved.getId(),
                "certificateNo", saved.getCertificateNo() != null ? saved.getCertificateNo() : ""
            ));
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
    public ResponseEntity<?> verifyRequestByVoucher(
            @PathVariable String voucherCode,
            @RequestParam(value = "mvcc", required = false) MultipartFile mvcc,
            @RequestParam(value = "mec", required = false) MultipartFile mec,
            @RequestParam(value = "mvcData", required = false) String mvcDataStr,
            @RequestParam(value = "mecData", required = false) String mecDataStr,
            Authentication auth) {
        try {
            Long userId = getUserId(auth);
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> mvcData = null;
            Map<String, Object> mecData = null;
            if (mvcDataStr != null && !mvcDataStr.isEmpty()) {
                mvcData = mapper.readValue(mvcDataStr, new TypeReference<Map<String, Object>>() {});
            }
            if (mecDataStr != null && !mecDataStr.isEmpty()) {
                mecData = mapper.readValue(mecDataStr, new TypeReference<Map<String, Object>>() {});
            }

            CertificateRequest saved = service.verifyRequestByVoucherCode(voucherCode, user, mvcData, mecData);
            return ResponseEntity.ok(Map.of("message", "Voucher verified successfully", "id", saved.getId(), "status", saved.getStatus()));
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
