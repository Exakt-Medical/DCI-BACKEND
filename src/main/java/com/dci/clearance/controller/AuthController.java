package com.dci.clearance.controller;

import com.dci.clearance.dto.CitizenRegisterRequest;
import com.dci.clearance.dto.LoginRequest;
import com.dci.clearance.dto.LoginResponse;
import com.dci.clearance.entity.User;
import com.dci.clearance.service.AccessLogService;
import com.dci.clearance.service.AuthService;
import com.dci.clearance.service.AuditTrailService;
import com.dci.clearance.service.CitizenRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final CitizenRegistrationService citizenRegistrationService;

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
    @Operation(summary = "Register a new citizen account", description = "Creates a new user with CITIZEN role and shadow company")
    public ResponseEntity<?> register(@Valid @RequestBody CitizenRegisterRequest request) {
        try {
            User user = citizenRegistrationService.registerCitizen(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Registration successful",
                    "username", user.getUsername(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "email", user.getEmail(),
                    "billerooSyncStatus", user.getBillerooSyncStatus()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}