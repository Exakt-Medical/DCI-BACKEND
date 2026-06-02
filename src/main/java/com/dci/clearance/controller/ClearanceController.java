package com.dci.clearance.controller;

import com.dci.clearance.dto.ClearanceRequestDto;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.service.ClearanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citizen")
@RequiredArgsConstructor
public class ClearanceController {

    private final ClearanceService clearanceService;
    private final UserRepository userRepository;

    @PostMapping("/clearance/create")
    public ResponseEntity<?> createDraft(Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = clearanceService.createDraft(userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/clearance/{id}/upload-orcr")
    public ResponseEntity<?> uploadOrCr(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam("ocrData") String ocrData,
                                         Authentication auth) {
        Long userId = getUserId(auth);
        try {
            ClearanceRequestDto dto = clearanceService.uploadOrCr(id, file.getBytes(), ocrData, userId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/clearance/{id}/vehicle-details")
    public ResponseEntity<?> updateVehicleDetails(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body,
                                                   Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = clearanceService.updateVehicleDetails(id,
                body.get("plateNumber"), body.get("mvFileNumber"),
                body.get("chassisNumber"), body.get("engineNumber"),
                body.get("make"), body.get("series"),
                body.get("yearModel"), body.get("color"),
                body.get("ownerName"), body.get("ownerAddress"), userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/clearance/{id}/payment-completed")
    public ResponseEntity<?> markPaymentCompleted(@PathVariable Long id, Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = clearanceService.markPaymentCompleted(id, userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/clearance/{id}/issue-voucher")
    public ResponseEntity<?> issueVoucher(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = clearanceService.issueVoucher(id, body.get("voucherCode"), userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/clearance/{id}/upload-mvcmec")
    public ResponseEntity<?> uploadMvcMec(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam("ocrData") String ocrData,
                                           Authentication auth) {
        Long userId = getUserId(auth);
        try {
            ClearanceRequestDto dto = clearanceService.uploadMvcMec(id, file.getBytes(), ocrData, userId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/clearance/{id}/issue-certificate")
    public ResponseEntity<?> issueCertificate(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = clearanceService.issueCertificate(id, body.get("certificateNo"), userId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/clearance/my-requests")
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        Long userId = getUserId(auth);
        List<ClearanceRequestDto> list = clearanceService.getMyRequests(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/clearance/{referenceNo}")
    public ResponseEntity<?> getByReferenceNo(@PathVariable String referenceNo) {
        try {
            ClearanceRequestDto dto = clearanceService.getByReferenceNo(referenceNo);
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