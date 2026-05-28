package com.exakt.vvip.controller;

import com.exakt.vvip.dto.LoginRequest;
import com.exakt.vvip.dto.LoginResponse;
import com.exakt.vvip.dto.RegisterRequest;
import com.exakt.vvip.service.AccessLogService;
import com.exakt.vvip.service.AuthService;
import com.exakt.vvip.service.AuditTrailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and registration")
public class AuthController {

    private final AuthService authService;
    private final AuditTrailService auditTrailService;
    private final AccessLogService accessLogService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password", description = "Returns JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            // Log successful login into both audit trail and access trail
            auditTrailService.logAction("Login", "User logged in successfully", request.getUsername(), response.getRole());
            accessLogService.logLogin(request.getUsername());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logs out the current user")
    public ResponseEntity<?> logout(Authentication auth) {
        if (auth != null) {
            String username = auth.getName();
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(g -> g.getAuthority().replace("ROLE_", ""))
                    .orElse("UNKNOWN");
            auditTrailService.logAction("Logout", "User logged out", username, role);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request");
        return ResponseEntity.status(400).body(Map.of("error", msg));
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Auth controller is working"));
    }

    @PostMapping("/echobody")
    public ResponseEntity<?> echoBody(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(body);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new company and user", description = "Registers a new insurance company with admin user")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(Map.of("message", "Registration submitted. Awaiting admin approval."));
    }
}