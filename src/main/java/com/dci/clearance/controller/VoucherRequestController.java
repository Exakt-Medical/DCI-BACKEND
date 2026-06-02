package com.dci.clearance.controller;

import com.dci.clearance.dto.ClearanceRequestDto;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.service.VoucherRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voucher-request")
@RequiredArgsConstructor
public class VoucherRequestController {

    private final VoucherRequestService voucherRequestService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createDraft(Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = voucherRequestService.createDraft(userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/upload-orcr")
    public ResponseEntity<?> uploadOrCr(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam("ocrData") String ocrData,
                                         Authentication auth) {
        Long userId = getUserId(auth);
        try {
            ClearanceRequestDto dto = voucherRequestService.uploadOrCr(id, file.getBytes(), ocrData, userId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/vehicle-details")
    public ResponseEntity<?> updateVehicleDetails(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body,
                                                   Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = voucherRequestService.updateVehicleDetails(id,
                body.get("plateNumber"), body.get("mvFileNumber"),
                body.get("chassisNumber"), body.get("engineNumber"),
                body.get("make"), body.get("series"),
                body.get("yearModel"), body.get("color"),
                body.get("ownerName"), body.get("ownerAddress"), userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/payment-completed")
    public ResponseEntity<?> markPaymentCompleted(@PathVariable Long id, Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = voucherRequestService.markPaymentCompleted(id, userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/issue-voucher")
    public ResponseEntity<?> issueVoucher(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = voucherRequestService.issueVoucher(id, body.get("voucherCode"), userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/assign-voucher")
    public ResponseEntity<?> assignVoucher(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            Authentication auth) {
        Long userId = getUserId(auth);
        ClearanceRequestDto dto = voucherRequestService.assignVoucher(id, body.get("voucherCode"), userId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        Long userId = getUserId(auth);
        List<ClearanceRequestDto> list = voucherRequestService.getMyRequests(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/agent-requests")
    public ResponseEntity<?> getAgentRequests(Authentication auth) {
        Long userId = getUserId(auth);
        List<ClearanceRequestDto> list = voucherRequestService.getByAgentFixer(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(voucherRequestService.getAll());
    }

    @GetMapping("/{referenceNo}")
    public ResponseEntity<?> getByReferenceNo(@PathVariable String referenceNo) {
        try {
            ClearanceRequestDto dto = voucherRequestService.getByReferenceNo(referenceNo);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/voucher/{voucherCode}")
    public ResponseEntity<?> getByVoucherCode(@PathVariable String voucherCode) {
        try {
            ClearanceRequestDto dto = voucherRequestService.getByVoucherCode(voucherCode);
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
