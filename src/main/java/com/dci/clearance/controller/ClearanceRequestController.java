package com.dci.clearance.controller;

import com.dci.clearance.dto.ClearanceRequestDto;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.service.ClearanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clearance-request")
@RequiredArgsConstructor
public class ClearanceRequestController {

    private final ClearanceRequestService clearanceRequestService;
    private final UserRepository userRepository;

    @PostMapping("/create-from-voucher/{voucherRequestId}")
    public ResponseEntity<?> createFromVoucher(@PathVariable Long voucherRequestId, Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = clearanceRequestService.createFromVoucher(voucherRequestId, userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/upload-mvcmec")
    public ResponseEntity<?> uploadMvcMec(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam("ocrData") String ocrData,
                                           Authentication auth) {
        Long userId = getUserId(auth);
        try {
            ClearanceRequestDto dto = clearanceRequestService.uploadMvcMec(id, file.getBytes(), ocrData, userId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/hpg-verify")
    public ResponseEntity<?> hpgVerify(@PathVariable Long id, Authentication auth) {
        Long hpgUserId = getUserId(auth);
        ClearanceRequestDto dto = clearanceRequestService.hpgVerify(id, hpgUserId);
        return ResponseEntity.ok(Map.of("message", "Vehicle verified successfully", "request", dto));
    }

    @PostMapping("/{id}/issue-certificate")
    public ResponseEntity<?> issueCertificate(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = clearanceRequestService.issueCertificate(id, body.get("certificateNo"), userId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        Long userId = getUserId(auth);
        List<ClearanceRequestDto> list = clearanceRequestService.getMyRequests(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/agent-requests")
    public ResponseEntity<?> getAgentRequests(Authentication auth) {
        Long userId = getUserId(auth);
        List<ClearanceRequestDto> list = clearanceRequestService.getByAgentFixer(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(clearanceRequestService.getAll());
    }

    @GetMapping("/{referenceNo}")
    public ResponseEntity<?> getByReferenceNo(@PathVariable String referenceNo) {
        try {
            ClearanceRequestDto dto = clearanceRequestService.getByReferenceNo(referenceNo);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/voucher/{voucherCode}")
    public ResponseEntity<?> getByVoucherCode(@PathVariable String voucherCode) {
        try {
            ClearanceRequestDto dto = clearanceRequestService.getByVoucherCode(voucherCode);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/certificate/{certificateNo}")
    public ResponseEntity<?> getByCertificateNo(@PathVariable String certificateNo) {
        try {
            ClearanceRequestDto dto = clearanceRequestService.getByCertificateNo(certificateNo);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Long getUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }
}
