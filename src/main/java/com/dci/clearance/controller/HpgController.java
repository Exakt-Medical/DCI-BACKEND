package com.dci.clearance.controller;

import com.dci.clearance.dto.ClearanceRequestDto;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.service.ClearanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hpg")
@RequiredArgsConstructor
public class HpgController {

    private final ClearanceRequestService clearanceRequestService;
    private final UserRepository userRepository;

    @GetMapping("/verify/{voucherCode}")
    public ResponseEntity<?> verifyByVoucher(@PathVariable String voucherCode) {
        try {
            ClearanceRequestDto dto = clearanceRequestService.getByVoucherCode(voucherCode);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "referenceNo", dto.getReferenceNo(),
                    "plateNumber", dto.getPlateNumber(),
                    "ownerName", dto.getOwnerName(),
                    "status", dto.getStatus(),
                    "hpgVerified", dto.getHpgVerified()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "No clearance found for this voucher"));
        }
    }

    @PostMapping("/verify/{requestId}")
    public ResponseEntity<?> verifyClearance(@PathVariable Long requestId, Authentication auth) {
        Long hpgUserId = getUserId(auth);
        ClearanceRequestDto dto = clearanceRequestService.hpgVerify(requestId, hpgUserId);
        return ResponseEntity.ok(Map.of("message", "Vehicle verified successfully", "request", dto));
    }

    private Long getUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }
}
