package com.exakt.vvip.controller;

import com.exakt.vvip.dto.VehicleVerificationRequest;
import com.exakt.vvip.dto.VehicleVerificationResponse;
import com.exakt.vvip.dto.VvsLookupResponse;
import com.exakt.vvip.security.UserDetailsImpl;
import com.exakt.vvip.service.DciCertificateService;
import com.exakt.vvip.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/vvip")
@RequiredArgsConstructor
@Tag(name = "VVIP", description = "Vehicle Verification Insurance Program")
@SecurityRequirement(name = "Bearer Authentication")
public class VvipController {

    private final VerificationService   verificationService;
    private final DciCertificateService dciCertificateService;

    @PostMapping("/verify")
    @Operation(summary = "Submit vehicle for VVIP verification")
    public ResponseEntity<VehicleVerificationResponse> verify(
            @Valid @RequestBody VehicleVerificationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        Long userId = resolveUserId(principal);
        VehicleVerificationResponse result = verificationService.verify(request, userId);
        return ResponseEntity.ok(result);
    }

    private Long resolveUserId(UserDetails principal) {
        if (principal instanceof UserDetailsImpl userDetailsImpl) {
            return userDetailsImpl.getId();
        }
        return null;
    }

    @PostMapping("/lookup")
    @Operation(summary = "Look up vehicle data from VVS without issuing a certificate")
    public ResponseEntity<VvsLookupResponse> lookup(
            @Valid @RequestBody VehicleVerificationRequest request) {
        VvsLookupResponse result = verificationService.lookup(request);
        return ResponseEntity.ok(result);
    }
}