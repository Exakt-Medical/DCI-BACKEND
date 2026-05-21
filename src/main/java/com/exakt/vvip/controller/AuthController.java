package com.exakt.vvip.controller;

import com.exakt.vvip.dto.*;
import com.exakt.vvip.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, 2FA verification, and registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password", description = "Returns JWT token or triggers 2FA")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-mfa")
    @Operation(summary = "Verify 2FA code", description = "Submit the 6-digit code sent to email to complete login")
    public ResponseEntity<LoginResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new company and user", description = "Registers a new insurance company with admin user")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(Map.of("message", "Registration submitted. Awaiting admin approval."));
    }
}